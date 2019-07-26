package com.github.sun.spider.spi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.sun.foundation.boot.utility.*;
import com.github.sun.spider.Fetcher;
import com.github.sun.spider.Spider;
import org.apache.commons.text.StringEscapeUtils;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.w3c.dom.Node;

import javax.script.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.github.sun.foundation.boot.utility.Tuple.Tuple2;

abstract class AbstractSpider implements Spider {
  private static final HtmlCleaner hc = new HtmlCleaner();
  private static final Fetcher fetcher = new JSoupFetcher();
  private static final ScriptEngineManager manager = new ScriptEngineManager();
  private Cache<JSON.Valuer, List<Field>> fieldsCache = new Cache<>();

  JsonNode parse(Node node, JSON.Valuer process) {
    Map<Path, List<Tuple2<Field, XPaths>>> parentPaths = new HashMap<>();
    JsonNode value = parse(0, null, node, process, parentPaths);
    parentPaths.forEach((path, xps) -> {
      List<String> paths = paths(path);
      JSON.Valuer valuer = JSON.newValuer(value, paths.toArray(new String[0]));
      if (valuer.raw().isArray()) {
        int i = 0;
        for (JSON.Valuer v : valuer.asArray()) {
          Tuple2<Field, XPaths> xp = xps.get(i++);
          put(xp._1, xp._2, (ObjectNode) v.raw());
        }
      } else if (!xps.isEmpty()) {
        Tuple2<Field, XPaths> xp = xps.get(0);
        put(xp._1, xp._2, (ObjectNode) valuer.raw());
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

  private JsonNode parse(int index, Path path, Node node, JSON.Valuer process, Map<Path, List<Tuple2<Field, XPaths>>> parentPaths) {
    String type = process.get("type").asText();
    String xpath = process.get("xpath").asText(null);
    List<Field> fields = fieldsCache.get(process, () -> parseFields(process));
    switch (type) {
      case "single":
        Node n = xpath == null ? node : XPaths.of(node, xpath).as();
        return parse(index, 0, path, node, n, fields, parentPaths);
      case "list":
        if (xpath == null) {
          throw new SpiderException("list type process require xpath");
        }
        List<Node> nodes = XPaths.of(node, xpath).asArray();
        List<Integer> indexes = parseIndexes(process, nodes.size());
        List<JsonNode> list = new ArrayList<>();
        for (int i = 0; i < indexes.size(); i++) {
          Node subNode = nodes.get(indexes.get(i));
          list.add(parse(index, i, path, node, subNode, fields, parentPaths));
        }
        return JSON.getMapper().createArrayNode().addAll(list);
      default:
        throw new SpiderException("unknown process type: " + type);
    }
  }

  private JsonNode parse(int index, int subIndex, Path path, Node parent, Node node, List<Field> fields, Map<Path, List<Tuple2<Field, XPaths>>> parentPaths) {
    ObjectNode value = JSON.getMapper().createObjectNode();
    for (Field field : fields) {
      XPaths xPaths = field.value.hasValue() ? null : XPaths.of(field.subXpath ? node : parent, field.xpath.path.value);
      if (xPaths != null && field.parent && path != null) {
        List<Tuple2<Field, XPaths>> xps = parentPaths.computeIfAbsent(path.parent, r -> new ArrayList<>());
        if (xps.size() <= index) {
          xps.add(Tuple.of(field, xPaths));
        }
      } else {
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
                value.putPOJO(field.name, parse(subIndex, p, node, field.subProcess, parentPaths));
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
                        nodes.add(parse(subIndex, p, n, field.subProcess, parentPaths));
                      } else {
                        Paging vp = uri.equals(baseUri) ? paging : parsePaging(get(req.set(uri)), field.subProcess);
                        Iterators.slice(vp.start, vp.end).forEach(i -> {
                          String url = pagingUrl(uri, i, vp);
                          Node n = get(req.set(url));
                          JsonNode values = parse(subIndex, p, n, field.subProcess, parentPaths);
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
                      JsonNode v = parse(subIndex, p, n, field.subProcess, parentPaths);
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
      }
    }
    return value;
  }

  private void put(Field field, XPaths xPaths, ObjectNode node) {
    String type = field.xpath.path.value;
    switch (field.type) {
      case "text":
        String t = field.value.hasValue() ?
          field.value.asText() : js(field.xpath.script, get(xPaths, type));
        node.put(field.name, t);
        break;
      case "int":
        int i = field.value.hasValue() ?
          field.value.asInt() : ((Number) js(field.xpath.script, get(xPaths, type))).intValue();
        node.put(field.name, i);
        break;
      case "long":
        long l = field.value.hasValue() ?
          field.value.asLong() : ((Number) js(field.xpath.script, get(xPaths, type))).longValue();
        node.put(field.name, l);
        break;
      case "double":
        double d = field.value.hasValue() ?
          field.value.asDouble() : ((Number) js(field.xpath.script, get(xPaths, type))).doubleValue();
        node.put(field.name, d);
        break;
      case "bool":
        boolean b = field.value.hasValue() ?
          field.value.asBoolean() : js(field.xpath.script, get(xPaths, type));
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

  @SuppressWarnings("unchecked")
  private <T> T js(String script, Object arg) {
    if (script == null) {
      return (T) arg;
    }
    try {
      ScriptEngine engine = manager.getEngineByName("JavaScript");
      ScriptContext context = new SimpleScriptContext();
      context.setAttribute("$value", arg, ScriptContext.ENGINE_SCOPE);
      engine.setContext(context);
      return (T) engine.eval(script);
    } catch (ScriptException ex) {
      throw new SpiderException("Error execute javascript: " + script, ex);
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

  private String parseUrl(String uri, Object replace, JSON.Valuer param) {
    if (param.raw().isTextual()) {
      return uri + String.format(param.asText(), replace);
    } else {
      String before = param.get("before").asText();
      String after = param.get("after").asText();
      return uri.substring(0, uri.indexOf(before)) + String.format(after, replace);
    }
  }

  Paging parsePaging(Node node, JSON.Valuer process) {
    JSON.Valuer paging = process.get("paging");
    if (paging.hasValue()) {
      int total = Integer.MAX_VALUE - 1;
      JSON.Valuer valuer = paging.get("total");
      if (valuer.hasValue()) {
        if (valuer.raw().isInt()) {
          total = valuer.asInt();
        } else if (valuer.raw().isTextual()) {
          total = XPaths.of(node, valuer.asText()).asInt();
        } else if (valuer.raw().isObject()) {
          String path = valuer.get("path").asText();
          String script = valuer.get("script").asText();
          total = ((Number) js(script, XPaths.of(node, path).asText())).intValue();
        } else {
          throw new SpiderException("paging.total is illegal: " + valuer.raw().toString());
        }
      }
      boolean exclusive = paging.get("exclusive").asBoolean(false);
      int max = exclusive ? total : total + 1;
      int end = paging.get("end").asInt(max);
      end = end > max ? max : end;
      int first = exclusive ? 0 : 1;
      int start = paging.get("start").asInt(first);
      start = start < first ? first : start;
      boolean includeFirst = paging.get("includeFirst").asBoolean(true);
      JSON.Valuer param = paging.get("param");
      return new Paging(first, start, end, includeFirst, param);
    }
    return null;
  }

  Category parseCategory(JSON.Valuer process) {
    JSON.Valuer category = process.get("category");
    if (category.hasValue()) {
      XPath xpath = parseXPath(category.get("xpath"));
      XPath subXpath = parseXPath(category.get("subXpath"));
      List<Integer> indexes = null;
      JSON.Valuer is = category.get("indexes");
      if (is.hasValue()) {
        indexes = new ArrayList<>();
        for (JSON.Valuer v : is.asArray()) {
          indexes.add(v.asInt());
        }
      }
      return new Category(xpath, subXpath, indexes);
    }
    return null;
  }

  List<Request> parseRequest(String uri, JSON.Valuer process) {
    int timeout = process.get("timeout").asInt(0);
    String method = process.get("method").asText("GET");
    String charset = process.get("charset").asText(StandardCharsets.UTF_8.name());
    JSON.Valuer body = process.get("body");
    List<Request> requests = new ArrayList<>();
    switch (method.toUpperCase()) {
      case "GET":
        requests.add(new Request(uri, timeout, method, charset, null));
        break;
      case "POST":
        if (body.hasValue()) {
          JSON.Valuer param = body.get("$param");
          if (param.hasValue()) {
            ObjectNode node = (ObjectNode) body.raw();
            node.remove("$param");
            String key = param.get("key").asText();
            if (node.has(key)) {
              int start = param.get("start").asInt();
              int end = param.get("end").asInt();
              String value = node.get(key).asText();
              Iterators.slice(start, end).forEach(replace -> {
                ObjectNode n = node.deepCopy();
                String newValue = String.format(value, replace);
                n.put(key, newValue);
                requests.add(new Request(uri, timeout, method, charset, n));
              });
            } else {
              requests.add(new Request(uri, timeout, method, charset, node));
            }
          }
        } else {
          requests.add(new Request(uri, timeout, method, charset, null));
        }
        break;
      default:
        throw new SpiderException("Unknown method: " + method);
    }
    return requests;
  }

  private List<Integer> parseIndexes(JSON.Valuer valuer, int defaultSize) {
    JSON.Valuer is = valuer.get("indexes");
    if (is.hasValue()) {
      List<Integer> indexes = new ArrayList<>();
      is.asArray().forEach(v -> {
        int i = v.asInt();
        if (i >= 0 && i < defaultSize) {
          indexes.add(i);
        }
      });
      return indexes;
    } else {
      return Iterators.slice(defaultSize);
    }
  }

  private List<Field> parseFields(JSON.Valuer process) {
    List<Field> fields = new ArrayList<>();
    process.get("fields").asArray().forEach(fv -> {
      String name = fv.get("name").asText();
      String type = fv.get("type").asText("text");
      XPath xpath = parseXPath(fv.get("xpath"), type);
      String subType = fv.get("subType").asText("local");
      boolean parent = fv.get("parent").asBoolean(false);
      boolean subXpath = fv.get("subXpath").asBoolean(true);
      JSON.Valuer subProcess = fv.get("subProcess");
      JSON.Valuer value = fv.get("value");
      fields.add(new Field(name, type, xpath, value, subType, parent, subXpath, subProcess));
    });
    return fields;
  }

  private XPath parseXPath(JSON.Valuer xp) {
    return parseXPath(xp, "text");
  }

  private XPath parseXPath(JSON.Valuer xp, String type) {
    if (xp.raw().isTextual()) {
      return new XPath(new XPath.Path(xp.asText(), "text"), null);
    } else if (xp.raw().isObject()) {
      String script = xp.get("script").asText();
      JSON.Valuer valuer = xp.get("path");
      XPath.Path path;
      if (valuer.raw().isObject()) {
        type = valuer.get("type").asText();
        String value = valuer.get("value").asText();
        path = new XPath.Path(type, value);
      } else {
        path = new XPath.Path(type, valuer.asText());
      }
      return new XPath(path, script);
    }
    return null;
  }

  private static class Path {
    private final Field field;
    private final Path parent;

    private Path(Field field, Path parent) {
      this.field = field;
      this.parent = parent;
    }

    private Path sub(Field field) {
      return new Path(field, this);
    }
  }

  private static class Field {
    private final String name;
    private final String type;
    private final XPath xpath;
    private final JSON.Valuer value;
    private final String subType;
    private final boolean parent;
    private final boolean subXpath;
    private final JSON.Valuer subProcess;

    private Field(String name, String type, XPath xpath, JSON.Valuer value, String subType, boolean parent, boolean subXpath, JSON.Valuer subProcess) {
      this.name = name;
      this.type = type;
      this.xpath = xpath;
      this.value = value;
      this.subType = subType;
      this.parent = parent;
      this.subXpath = subXpath;
      this.subProcess = subProcess;
    }
  }

  static class Paging {
    final int first;
    final int start;
    final int end;
    final boolean includeFirst;
    final JSON.Valuer param;

    private Paging(int first, int start, int end, boolean includeFirst, JSON.Valuer param) {
      this.first = first;
      this.start = start;
      this.end = end;
      this.includeFirst = includeFirst;
      this.param = param;
    }
  }

  // 只解析一级分类
  static class Category {
    private final XPath xpath;
    private final XPath subXpath;
    private final List<Integer> indexes;

    private Category(XPath xpath, XPath subXpath, List<Integer> indexes) {
      this.xpath = xpath;
      this.subXpath = subXpath;
      this.indexes = indexes;
    }
  }

  static class Request {
    private final String uri;
    private final int timeout;
    private final String method;
    private final String charset;
    private final JsonNode body;

    private Request(String uri, int timeout, String method, String charset, JsonNode body) {
      this.uri = uri;
      this.timeout = timeout;
      this.method = method;
      this.charset = charset;
      this.body = body;
    }

    Request set(String uri) {
      return new Request(uri, this.timeout, this.method, this.charset, this.body);
    }
  }

  private static class XPath {
    private final Path path;
    private final String script;

    private XPath(Path path, String script) {
      this.path = path;
      this.script = script;
    }

    private static class Path {
      private final String type;
      private final String value;

      private Path(String type, String value) {
        this.type = type;
        this.value = value;
      }
    }
  }
}
