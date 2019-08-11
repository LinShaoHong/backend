package com.github.sun.spider.spi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.sun.foundation.boot.utility.Cache;
import com.github.sun.foundation.boot.utility.Iterators;
import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.foundation.boot.utility.Retry;
import com.github.sun.spider.Fetcher;
import com.github.sun.spider.Spider;
import org.apache.commons.text.StringEscapeUtils;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.w3c.dom.Node;

import java.util.*;
import java.util.stream.Collectors;

abstract class AbstractSpider extends SchemaParser implements Spider {
  private static final HtmlCleaner hc = new HtmlCleaner();
  private static final Fetcher fetcher = new JSoupFetcher();
  private Cache<JSON.Valuer, List<Field>> fieldsCache = new Cache<>();

  JsonNode crawl(Node node, JSON.Valuer process) {
    Map<Path, Map<Field, List<XPaths>>> parentPaths = new HashMap<>();
    JsonNode value = crawl(0, null, node, process, parentPaths);
    parentPaths.forEach((path, xps) -> {
      List<String> paths = paths(path);
      JSON.Valuer valuer = JSON.newValuer(value, paths.toArray(new String[0]));
      if (valuer.raw().isArray()) {
        xps.forEach((field, values) -> {
          int i = 0;
          for (JSON.Valuer v : valuer.asArray()) {
            put(field, values.get(i++), (ObjectNode) v.raw());
          }
        });
      } else if (!xps.isEmpty()) {
        xps.forEach((field, values) -> put(field, values.get(0), (ObjectNode) valuer.raw()));
      }
    });
    return value;
  }

  private JsonNode crawl(int index, Path path, Node node, JSON.Valuer process, Map<Path, Map<Field, List<XPaths>>> parentPaths) {
    String type = process.get("type").asText();
    String xpath = process.get("xpath").asText(null);
    List<Field> fields = fieldsCache.get(process, () -> parseFields(process));
    switch (type) {
      case "single":
        Node n = xpath == null ? node : XPaths.of(node, xpath).as();
        return crawl(index, 0, path, node, n, fields, parentPaths);
      case "list":
        if (xpath == null) {
          throw new SpiderException("list type process require xpath");
        }
        List<Node> nodes = XPaths.of(node, xpath).asArray();
        List<Integer> indexes = parseIndexes(process, nodes.size());
        List<JsonNode> list = new ArrayList<>();
        for (int i = 0; i < indexes.size(); i++) {
          Node subNode = nodes.get(indexes.get(i));
          list.add(crawl(index, i, path, node, subNode, fields, parentPaths));
        }
        return JSON.getMapper().createArrayNode().addAll(list);
      default:
        throw new SpiderException("unknown process type: " + type);
    }
  }

  private JsonNode crawl(int index, int subIndex, Path path, Node parent, Node node, List<Field> fields, Map<Path, Map<Field, List<XPaths>>> parentPaths) {
    ObjectNode value = JSON.getMapper().createObjectNode();
    if (path != null) {
      Map<Field, List<XPaths>> xps = parentPaths.computeIfAbsent(path.parent, r -> new HashMap<>());
      List<Field> withParentFields = fields.stream().filter(field -> field.parent).collect(Collectors.toList());
      if (!withParentFields.isEmpty()) {
        withParentFields.forEach(field -> {
          List<XPaths> tps = xps.computeIfAbsent(field, r -> new ArrayList<>());
          XPaths xPaths = field.value.hasValue() ? null : XPaths.of(field.subXpath ? node : parent, field.xpath.path.value);
          if (tps.size() <= index) {
            tps.add(xPaths);
          }
        });
      }
    }
    fields.stream().filter(field -> !field.parent)
      .forEach(field -> {
        XPaths xPaths = field.value.hasValue() ? null : XPaths.of(field.subXpath ? node : parent, field.xpath.path.value);
        switch (field.type) {
          case "text":
          case "int":
          case "long":
          case "double":
          case "bool":
            put(field, xPaths, value);
            break;
          case "$sub":
            Path p = path == null ? new Path(field, null) : path.sub(field);
            switch (field.subType) {
              case "local":
                value.putPOJO(field.name, crawl(subIndex, p, node, field.subProcess, parentPaths));
                break;
              case "href":
                String baseUri = xPaths == null ? field.value.asText() : xPaths.asText();
                String method = field.subProcess.get("method").asText("GET");
                Category category = parseCategory(field.subProcess);
                List<Request> requests = parseRequest(baseUri, field.subProcess);
                switch (method.toUpperCase()) {
                  case "GET":
                    Request req = requests.get(0);
                    Node subNode = get(req);
                    Paging paging = parsePaging(subNode, field.subProcess);
                    List<String> uris = category == null ? Collections.singletonList(baseUri) : categoryUrl(subNode, category);
                    ArrayNode nodes = JSON.getMapper().createArrayNode();
                    for (String uri : uris) {
                      if (paging == null) {
                        Node n = uri.equals(baseUri) ? subNode : get(req.set(uri));
                        nodes.add(crawl(subIndex, p, n, field.subProcess, parentPaths));
                      } else {
                        Paging vp = uri.equals(baseUri) ? paging : parsePaging(get(req.set(uri)), field.subProcess);
                        Iterators.slice(vp.start, vp.end).forEach(i -> {
                          String url = pagingUrl(uri, i, vp);
                          Node n = get(req.set(url));
                          JsonNode values = crawl(subIndex, p, n, field.subProcess, parentPaths);
                          if (values.isArray()) {
                            nodes.addAll((ArrayNode) values);
                          } else {
                            nodes.add(values);
                          }
                        });
                      }
                    }
                    value.putPOJO(field.name, (category == null && paging == null) ? nodes.get(0) : nodes);
                    break;
                  case "POST":
                    uris = category == null ?
                      Collections.singletonList(baseUri) : categoryUrl(get(baseUri), category);
                    nodes = JSON.getMapper().createArrayNode();
                    uris.forEach(uri -> parseRequest(uri, field.subProcess).forEach(r -> {
                      Node n = get(r);
                      JsonNode v = crawl(subIndex, p, n, field.subProcess, parentPaths);
                      nodes.add(v);
                    }));
                    value.putPOJO(field.name, category == null ? nodes.get(0) : nodes);
                    break;
                  default:
                    throw new SpiderException("Unknown method: " + method);
                }
                break;
              default:
                throw new SpiderException("unknown field subType of " + field.name + " with '" + field.subType + "'");
            }
            break;
          default:
            throw new SpiderException("unknown field type of " + field.name + " with '" + field.name + "'");
        }
      });
    return value;
  }

  private List<String> paths(Path path) {
    List<String> list = new ArrayList<>();
    Path p = path;
    while (p != null) {
      list.add(p.field.name);
      p = p.parent;
    }
    Collections.reverse(list);
    return list;
  }

  Node get(String url) {
    try {
      return Retry.execute(getSetting().getRetryCount(), getSetting().getRetryDelays(),
        () -> new DomSerializer(new CleanerProperties()).createDOM(hc.clean(fetcher.fetch(url))));
    } catch (Exception ex) {
      throw new SpiderException("Error get html from url: " + url, ex);
    }
  }

  Node get(Request req) {
    try {
      return Retry.execute(getSetting().getRetryCount(), getSetting().getRetryDelays(), () -> {
        String body = fetcher.fetch(req.uri, req.timeout, req.method, req.body, req.charset);
        return new DomSerializer(new CleanerProperties()).createDOM(hc.clean(body));
      });
    } catch (Exception ex) {
      throw new SpiderException("Error get html from url: " + req.uri, ex);
    }
  }

  private void put(Field field, XPaths xPaths, ObjectNode node) {
    switch (field.type) {
      case "text":
        String t = field.value.hasValue() ?
          field.value.asText() : js(field.xpath.script, get(xPaths, field.xpath.path.type));
        node.put(field.name, t);
        break;
      case "int":
        int i = field.value.hasValue() ?
          field.value.asInt() : ((Number) js(field.xpath.script, get(xPaths, field.xpath.path.type))).intValue();
        node.put(field.name, i);
        break;
      case "long":
        long l = field.value.hasValue() ?
          field.value.asLong() : ((Number) js(field.xpath.script, get(xPaths, field.xpath.path.type))).longValue();
        node.put(field.name, l);
        break;
      case "double":
        double d = field.value.hasValue() ?
          field.value.asDouble() : ((Number) js(field.xpath.script, get(xPaths, field.xpath.path.type))).doubleValue();
        node.put(field.name, d);
        break;
      case "bool":
        boolean b = field.value.hasValue() ?
          field.value.asBoolean() : js(field.xpath.script, get(xPaths, field.xpath.path.type));
        node.put(field.name, b);
        break;
      default:
        throw new SpiderException("Unknown type: " + field.type);
    }
  }

  private Object get(XPaths xPaths, String type) {
    switch (type) {
      case "text":
        return StringEscapeUtils.unescapeHtml4(xPaths.asText());
      case "int":
        return xPaths.asInt();
      case "long":
        return xPaths.asLong();
      case "double":
        return xPaths.asDouble();
      case "bool":
        return xPaths.asBoolean();
      default:
        throw new SpiderException("Unknown type: " + type);
    }
  }

  int getEntityNum(String type, String xpath, Node node, JSON.Valuer process) {
    switch (type) {
      case "single":
        return 1;
      case "list":
        if (xpath == null) {
          throw new SpiderException("list type process require xpath");
        }
        List<Node> nodes = XPaths.of(node, xpath).asArray();
        List<Integer> indexes = parseIndexes(process, nodes.size());
        return indexes.size();
      default:
        return 0;
    }
  }

  List<String> categoryUrl(Node node, Category category) {
    List<String> total = new ArrayList<>();
    XPaths.of(node, category.xpath.path.value).asArray()
      .forEach(n -> {
        XPaths xPaths = XPaths.of(n, category.subXpath.path.value);
        Object value = get(xPaths, category.subXpath.path.type);
        String url = js(category.subXpath.script, value);
        total.add(url);
      });
    if (category.indexes != null) {
      List<String> urls = new ArrayList<>();
      category.indexes.forEach(i -> {
        if (i >= 0 && i < total.size()) {
          urls.add(total.get(i));
        }
      });
      return urls;
    }
    return total;
  }

  String pagingUrl(String uri, int page, Paging paging) {
    if (page == paging.first && !paging.includeFirst) {
      return uri;
    }
    return parseUrl(uri, page, paging.param);
  }
}
