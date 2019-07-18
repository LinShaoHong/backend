package com.github.sun.spider.spi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.sun.foundation.boot.utility.Cache;
import com.github.sun.foundation.boot.utility.Iterators;
import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.foundation.boot.utility.Tuple;
import org.apache.commons.text.StringEscapeUtils;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.jsoup.Jsoup;
import org.w3c.dom.Node;

import java.util.*;

import static com.github.sun.foundation.boot.utility.Tuple.Tuple2;

abstract class AbstractSpider implements Spider {
  private static final HtmlCleaner hc = new HtmlCleaner();
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

  Node parse(String url) {
    try {
      String body = Jsoup.connect(url).get().body().html();
      TagNode tn = hc.clean(body);
      return new DomSerializer(new CleanerProperties()).createDOM(tn);
    } catch (Exception ex) {
      throw new SpiderException("Error get html from url: " + url, ex);
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
      XPaths xPaths = XPaths.of(field.subXpath ? node : parent, field.xpath);
      if (field.parent && path != null) {
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
                String baseUri = xPaths.asText();
                Node subNode = parse(baseUri);
                Category category = parseCategory(field.subProcess);
                Paging paging = parsePaging(subNode, field.subProcess);
                List<String> uris = category == null ? Collections.singletonList(baseUri) : categoryUrl(subNode, category);
                for (String uri : uris) {
                  if (paging == null) {
                    Node n = uri.equals(baseUri) ? subNode : parse(uri);
                    value.putPOJO(field.name, parse(subIndex, p, n, field.subProcess, parentPaths));
                  } else {
                    Paging vp = uri.equals(baseUri) ? paging : parsePaging(parse(uri), field.subProcess);
                    List<JsonNode> nodes = new ArrayList<>();
                    Iterators.slice(vp.start, vp.end).forEach(i -> {
                      String url = pagingUrl(uri, i, vp);
                      Node n = parse(url);
                      JsonNode values = parse(subIndex, p, n, field.subProcess, parentPaths);
                      if (values.isArray()) {
                        values.forEach(nodes::add);
                      } else {
                        nodes.add(values);
                      }
                    });
                    value.putPOJO(field.name, nodes);
                  }
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
    switch (field.type) {
      case "text":
        node.put(field.name, StringEscapeUtils.unescapeHtml4(xPaths.asText()));
        break;
      case "int":
        node.put(field.name, xPaths.asInt());
        break;
      case "long":
        node.put(field.name, xPaths.asLong());
        break;
      case "double":
        node.put(field.name, xPaths.asDouble());
        break;
      case "bool":
        node.put(field.name, xPaths.asBoolean());
        break;
      default:
        // do nothing
    }
  }

  List<String> categoryUrl(Node node, Category category) {
    List<String> total = new ArrayList<>();
    XPaths.of(node, category.xpath).asArray()
      .forEach(n -> total.add(XPaths.of(n, category.subXpath).asText()));
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
      String xpath = paging.get("total").asText();
      int total = XPaths.of(node, xpath).asInt();
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
      String xpath = category.get("xpath").asText();
      String subXpath = category.get("subXpath").asText();
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
      String xpath = fv.get("xpath").asText();
      String subType = fv.get("subType").asText("local");
      boolean parent = fv.get("parent").asBoolean(false);
      boolean subXpath = fv.get("subXpath").asBoolean(true);
      JSON.Valuer subProcess = fv.get("subProcess");
      fields.add(new Field(name, type, xpath, subType, parent, subXpath, subProcess));
    });
    return fields;
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
    private final String xpath;
    private final String subType;
    private final boolean parent;
    private final boolean subXpath;
    private final JSON.Valuer subProcess;

    private Field(String name, String type, String xpath, String subType, boolean parent, boolean subXpath, JSON.Valuer subProcess) {
      this.name = name;
      this.type = type;
      this.xpath = xpath;
      this.subType = subType;
      this.parent = parent;
      this.subXpath = subXpath;
      this.subProcess = subProcess;
    }
  }

  static class Paging {
    public final int first;
    public final int start;
    public final int end;
    public final boolean includeFirst;
    public final JSON.Valuer param;

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
    private final String xpath;
    private final String subXpath;
    private final List<Integer> indexes;

    private Category(String xpath, String subXpath, List<Integer> indexes) {
      this.xpath = xpath;
      this.subXpath = subXpath;
      this.indexes = indexes;
    }
  }
}
