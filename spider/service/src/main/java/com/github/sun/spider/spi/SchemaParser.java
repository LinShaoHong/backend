package com.github.sun.spider.spi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.sun.foundation.boot.utility.Iterators;
import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.spider.SpiderException;
import com.github.sun.spider.XPaths;
import org.w3c.dom.Node;

import javax.script.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

abstract class SchemaParser {
  private static final ScriptEngineManager manager = new ScriptEngineManager();
  private static final int FETCH_TIME_OUT = 5000;

  Paging parsePaging(String baseUri, Node node, JSON.Valuer process) {
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
      String uri = baseUri;
      valuer = paging.get("xpath");
      if (valuer.raw().isTextual()) {
        uri = XPaths.of(node, valuer.asText()).asText();
      } else if (valuer.raw().isObject()) {
        String path = valuer.get("path").asText();
        String script = valuer.get("script").asText();
        uri = js(script, XPaths.of(node, path).asText());
      }

      boolean isStatic = paging.get("static").asBoolean(false);
      boolean exclusive = paging.get("exclusive").asBoolean(false);
      int max = exclusive ? total : total + 1;
      int end = paging.get("end").asInt(max);
      end = Math.min(end, max);
      int first = exclusive ? 0 : 1;
      int start = paging.get("start").asInt(first);
      start = Math.max(start, first);
      boolean includeFirst = paging.get("includeFirst").asBoolean(true);
      JSON.Valuer param = paging.get("param");
      return new Paging(node, uri, first, start, end, isStatic, includeFirst, param);
    }
    return null;
  }

  <T> T js(String script, Object arg) {
    return js(script, 0, arg);
  }

  @SuppressWarnings("unchecked")
  <T> T js(String script, int currPage, Object arg) {
    if (script == null) {
      return (T) arg;
    }
    try {
      ScriptEngine engine = manager.getEngineByName("JavaScript");
      ScriptContext context = new SimpleScriptContext();
      context.setAttribute("$value", arg, ScriptContext.ENGINE_SCOPE);
      context.setAttribute("$currPage", currPage, ScriptContext.ENGINE_SCOPE);
      engine.setContext(context);
      return (T) engine.eval(script);
    } catch (ScriptException ex) {
      throw new SpiderException("Error execute javascript: " + script, ex);
    }
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
    int timeout = process.get("timeout").asInt(FETCH_TIME_OUT);
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

  List<Integer> parseIndexes(JSON.Valuer valuer, int defaultSize) {
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

  List<Field> parseFields(JSON.Valuer process) {
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
      return new XPath(new XPath.Path("text", xp.asText()), null);
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

  static class Path {
    final Field field;
    final Path parent;

    Path(Field field, Path parent) {
      this.field = field;
      this.parent = parent;
    }

    Path sub(Field field) {
      return new Path(field, this);
    }
  }

  static class Field {
    final String name;
    final String type;
    final XPath xpath;
    final JSON.Valuer value;
    final String subType;
    final boolean parent;
    final boolean subXpath;
    final JSON.Valuer subProcess;

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

  static class Paging implements Iterable<String> {
    private final Node node;
    private final String baseUri;
    private final int first;
    private final int start;
    private final int end;
    private final boolean isStatic;
    private final int[] curr;
    private final boolean includeFirst;
    private final JSON.Valuer param;

    private Paging(Node node, String baseUri, int first, int start, int end, boolean isStatic, boolean includeFirst, JSON.Valuer param) {
      this.node = node;
      this.baseUri = baseUri;
      this.first = first;
      this.start = start;
      this.end = end;
      this.isStatic = isStatic;
      this.curr = new int[1];
      this.includeFirst = includeFirst;
      this.param = param;
    }

    public Node getNode() {
      return node;
    }

    public int getCurrPage() {
      return curr[0];
    }

    public boolean isStatic() {
      return isStatic;
    }

    @Override
    public Iterator<String> iterator() {
      int[] cr = new int[1];
      cr[0] = start - 1;

      return new Iterator<>() {
        @Override
        public boolean hasNext() {
          return cr[0] < end;
        }

        @Override
        public String next() {
          cr[0]++;
          curr[0] = cr[0];
          if (cr[0] == first && !includeFirst) {
            return baseUri;
          }
          if (!param.hasValue()) {
            return baseUri;
          } else if (param.raw().isTextual()) {
            return baseUri + String.format(param.asText(), cr[0]);
          } else {
            String before = param.get("before").asText();
            String after = param.get("after").asText();
            return baseUri.substring(0, baseUri.indexOf(before)) + String.format(after, cr[0]);
          }
        }
      };
    }
  }

  // 只解析一级分类
  static class Category {
    final XPath xpath;
    final XPath subXpath;
    final List<Integer> indexes;

    private Category(XPath xpath, XPath subXpath, List<Integer> indexes) {
      this.xpath = xpath;
      this.subXpath = subXpath;
      this.indexes = indexes;
    }
  }

  static class Request {
    final String uri;
    final int timeout;
    final String method;
    final String charset;
    final JsonNode body;

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

  static class XPath {
    final Path path;
    final String script;

    private XPath(Path path, String script) {
      this.path = path;
      this.script = script;
    }

    static class Path {
      final String type;
      final String value;

      private Path(String type, String value) {
        this.type = type;
        this.value = value;
      }
    }
  }
}
