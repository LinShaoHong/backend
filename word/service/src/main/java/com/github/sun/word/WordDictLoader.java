package com.github.sun.word;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.sun.foundation.ai.Assistant;
import com.github.sun.foundation.boot.Scanner;
import com.github.sun.foundation.boot.exception.ConstraintException;
import com.github.sun.foundation.boot.utility.Dates;
import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.foundation.boot.utility.Reflections;
import com.github.sun.foundation.sql.IdGenerator;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.spider.Fetcher;
import com.github.sun.spider.XPaths;
import com.github.sun.word.loader.*;
import com.github.sun.word.spider.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RefreshScope
public class WordDictLoader {
  private static final HtmlCleaner hc = new HtmlCleaner();
  private final static ExecutorService executor = Executors.newFixedThreadPool(10);
  private final static Cache<String, Document> documents = Caffeine.newBuilder()
    .expireAfterWrite(10, TimeUnit.MINUTES)
    .maximumSize(100)
    .build();

  @Value("${qwen.key}")
  private String apiKey;
  @Value("${qwen.model}")
  private String model;
  @Resource(name = "qwen")
  private Assistant assistant;
  @Resource(name = "mysql")
  protected SqlBuilder.Factory factory;
  @Resource
  private WordDictMapper mapper;
  @Resource
  private WordDictDiffMapper diffMapper;
  @Resource
  private WordLoaderCheckMapper checkMapper;
  @Resource
  private WordLoaderCodeMapper codeMapper;
  @Resource
  private WordLoaderAffixMapper affixMapper;
  @Resource
  private WordDictTreeMapper treeMapper;
  @Resource
  private WordLoaderTagMapper tagMapper;
  @Resource
  private WordLoaderExistMapper existMapper;

  public String chat(String q) {
    return assistant.chat(apiKey, model, q);
  }

  public void fetch(int userId) {
    SqlBuilder sb = factory.create();
    SqlBuilder.Template template = sb.from(WordLoaderTag.class).limit(300, 500).template();
    tagMapper.findByTemplate(template).forEach(tag -> {
      loadAll(tag.getId(), userId);
    });
  }

  public void loadAll(String words, int userId) {
    for (String word : words.split(",")) {
      WordBasicLoader.init(word, userId);
      Scanner.getClassesWithInterface(WordLoader.class)
        .stream()
        .filter(Scanner.ClassTag::isImplementClass)
        .filter(v -> v.runtimeClass() != WordDerivativesLoader.class)
        .forEach(loader -> executor.submit(() -> loader.getInstance().load(word, userId)));
    }
  }

  public void loadPart(String word, String part, JsonNode attr, int userId) {
    WordBasicLoader.init(word, userId);
    mapper.loading(word, "'$." + part + "Loading'");
    Scanner.getClassesWithInterface(WordLoader.class)
      .stream().filter(v -> v.isImplementClass() && v.runtimeClass().isAnnotationPresent(Service.class) &&
        v.runtimeClass().getAnnotation(Service.class).value().equals(part))
      .findFirst()
      .ifPresent(loader -> executor.submit(() -> loader.getInstance().load(word, attr == null ? null : JSON.newValuer(attr), userId)));
  }

  @Transactional
  public void removePart(String word, String part, String path, JsonNode attr, int userId) {
    WordDict dict = WordBasicLoader.init(word, userId);
    switch (part) {
      case "meaning":
        if (StringUtils.hasText(path)) {
          Reflections.setValue(dict.getMeaning(), path, "");
        } else {
          dict.setMeaning(new WordDict.TranslatedMeaning());
        }
        dict.setPassed(false);
        mapper.update(dict);
        break;
      case "struct":
        if (!StringUtils.hasText(path)) {
          dict.setStruct(null);
        }
        dict.setPassed(false);
        mapper.update(dict);
        break;
      case "inflection":
        if (StringUtils.hasText(path)) {
          Reflections.setValue(dict.getInflection(), path, Collections.emptyList());
        } else {
          dict.setInflection(new WordDict.Inflection());
        }
        dict.setPassed(false);
        mapper.update(dict);
        break;
      case "collocation":
        if (StringUtils.hasText(path)) {
          String removed = path.endsWith(":phrase") ? path.substring(0, path.length() - 7) : path;
          if (path.endsWith(":phrase")) {
            dict.getCollocation().getPhrases().removeIf(d -> Objects.equals(d.getEn(), removed));
          } else {
            dict.getCollocation().getFormulas().removeIf(d -> Objects.equals(d.getEn(), removed));
          }
        } else {
          dict.setCollocation(null);
        }
        dict.setPassed(false);
        mapper.update(dict);
        break;
      case "derivatives":
        if (StringUtils.hasText(path)) {
          String removed = path.endsWith(":sub") ? path.substring(0, path.length() - 4) : path;
          String treeId = attr.get("treeId").asText();
          int version = attr.get("version").asInt();
          WordDictTree tree = treeMapper.findById(treeId);
          List<WordDictTree.Derivative> vs = tree.getDerivatives();
          int j = -1;
          for (int i = 0; i < vs.size(); i++) {
            if (vs.get(i).getWord().equals(removed)) {
              j = i;
              break;
            }
          }
          if (j >= 0) {
            int z = j;
            for (int k = j + 1; k < vs.size(); k++) {
              if (vs.get(k).getIndex() <= vs.get(j).getIndex()) {
                break;
              }
              z = k;
            }
            if (path.endsWith(":sub")) {
              List<WordDictTree.Derivative> ds = new ArrayList<>();
              for (int i = j; i <= z; i++) {
                ds.add(vs.get(i));
              }
              vs.removeAll(ds);
            } else {
              for (int i = j + 1; i <= z; i++) {
                vs.get(i).setIndex(vs.get(i).getIndex() - 1);
              }
              vs.remove(j);
            }
          }
          editTree(tree.getRoot(), tree.getRootDesc(), version, vs);
        }
        break;
      case "differs":
        if (StringUtils.hasText(path)) {
          dict.getDiffers().removeIf(d -> Objects.equals(d, path));
        } else {
          dict.setDiffers(Collections.emptyList());
        }
        dict.setPassed(false);
        mapper.update(dict);
        break;
      case "synonym":
        if (StringUtils.hasText(path)) {
          dict.getSynAnts().getSynonyms().removeIf(d -> Objects.equals(d, path));
        } else {
          dict.getSynAnts().setSynonyms(new ArrayList<>());
        }
        dict.setPassed(false);
        mapper.update(dict);
        break;
      case "antonym":
        if (StringUtils.hasText(path)) {
          dict.getSynAnts().getAntonyms().removeIf(d -> Objects.equals(d, path));
        } else {
          dict.getSynAnts().setAntonyms(new ArrayList<>());
        }
        dict.setPassed(false);
        mapper.update(dict);
        break;
      case "tree":
        treeMapper.deleteById(path);
        break;
      default:
        break;
    }
  }

  @Transactional
  public int remove(String word) {
    int next = 1;
    WordDict dict = mapper.findById(word);
    if (dict != null) {
      String date = Dates.format(dict.getLoadTime());
      mapper.dec(dict.getSort(), date);
      mapper.deleteById(word);
      next = dict.getSort();
      int total = mapper.countByLoadTime(date);
      if (total < next) {
        next = total;
      }
      codeMapper.updateById(date + ":1", total);
    }
    return next;
  }

  @Transactional
  public void pass(String word) {
    mapper.pass(word);
  }

  @Transactional
  public WordLoaderCheck stat(String date, int userId) {
    WordLoaderCheck check = checkMapper.findById(date + ":" + userId);
    check.setTotal(mapper.countByLoadTime(date));
    check.setPassed(mapper.countByPassed(date));
    check.setViewed(mapper.countByViewed(date));
    return check;
  }

  @Transactional
  public List<WordLoaderCheck> stats(int userId) {
    List<String> dates = checkMapper.dates();
    return dates.stream().map(date -> {
      String id = date + ":" + userId;
      WordLoaderCheck check = checkMapper.findById(id);
      if (check == null) {
        check = new WordLoaderCheck();
        check.setId(date + ":" + userId);
        check.setUserId(userId);
        check.setDate(date);
        check.setSort(1);
        checkMapper.replace(check);
      }
      check.setTotal(mapper.countByLoadTime(date));
      check.setPassed(mapper.countByPassed(date));
      check.setViewed(mapper.countByViewed(date));
      return check;
    }).collect(Collectors.toList());
  }

  @Transactional
  public WordDict dict(String date, Integer sort, int userId) {
    WordLoaderCheck check = null;
    List<WordLoaderCheck> all = stats(userId);
    if (!StringUtils.hasText(date)) {
      check = all.stream().filter(WordLoaderCheck::isCurr).findFirst().orElse(null);
      if (check == null) {
        check = all.get(all.size() - 1);
      }
      date = check.getDate();
    }
    check = check == null ? checkMapper.findById(date + ":" + userId) : check;
    if (sort == null) {
      sort = check.getSort();
    }
    WordDict dict = mapper.byDateAndSort(date, sort);
    if (dict != null) {
      check.setSort(sort);
      check.setCurr(true);
      checkMapper.update(check);
      checkMapper.past(date + ":" + userId, userId);

      mapper.viewed(dict.getId());
    }
    return dict;
  }

  public List<WordDict> dicts(String date) {
    return mapper.byDate(date);
  }

  public String root(String word) {
    WordLoaderAffix affix = affixMapper.findById(word);
    return affix == null ? "" : affix.getRoot();
  }

  public WordLoaderAffix affix(String word) {
    return affixMapper.byId(word);
  }

  public static synchronized Document fetchDocument(String url) {
    return documents.get(url, u -> {
      try {
        String html = Fetcher.fetch(url);
        return new DomSerializer(new CleanerProperties()).createDOM(hc.clean(html));
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    });
  }

  public void editMeaning(String id, WordDict.TranslatedMeaning meaning) {
    SqlBuilder sb = factory.create();
    SqlBuilder.Template template = sb.from(WordDict.class)
      .where(sb.field("id").eq(id))
      .update()
      .set("passed", 0)
      .set("meaning", meaning)
      .template();
    mapper.updateByTemplate(template);
  }

  public void editStruct(String id, WordDict.Struct struct) {
    struct.getParts().removeIf(v -> !StringUtils.hasText(v.getPart()));
    SqlBuilder sb = factory.create();
    SqlBuilder.Template template = sb.from(WordDict.class)
      .where(sb.field("id").eq(id))
      .update()
      .set("passed", 0)
      .set("struct", struct)
      .template();
    mapper.updateByTemplate(template);
  }

  public WordDictTree moveDerivative(String id, int version, String word, String op) {
    WordDictTree tree = treeMapper.findById(id);
    List<WordDictTree.Derivative> list = tree.getDerivatives();
    List<WordDictTree.Derivative> derivatives = new ArrayList<>();
    class Util {
      public List<Node> make(String parent, int pos, int index) {
        List<Node> cs = new ArrayList<>();
        for (int i = pos; i < list.size(); i++) {
          WordDictTree.Derivative v = list.get(i);
          if (v.getIndex() < index) {
            break;
          }
          if (v.getIndex() == index) {
            Node node = new Node();
            node.setWord(v.getWord());
            node.setVersion(v.getVersion());
            node.setMerged(v.isMerged());
            node.setParent(parent);
            node.setChildren(make(node.getWord(), i + 1, v.getIndex() + 1));
            cs.add(node);
          }
        }
        return cs;
      }

      public void walk(Node node, int index) {
        derivatives.add(new WordDictTree.Derivative(node.getWord(), index, node.getVersion(), node.isMerged()));
        node.getChildren().forEach(c -> walk(c, index + 1));
      }

      public Node find(List<Node> ns, String word) {
        for (Node n : ns) {
          if (Objects.equals(n.getWord(), word)) {
            return n;
          } else {
            List<Node> cs = n.getChildren();
            Node c = find(cs, word);
            if (c != null) {
              return c;
            }
          }
        }
        return null;
      }
    }
    Util util = new Util();
    List<Node> nodes = util.make(null, 0, 0);
    Node curr = util.find(nodes, word);
    if (curr != null) {
      Node parent = StringUtils.hasText(curr.getParent()) ?
        util.find(nodes, curr.getParent()) : null;
      Node grandParent = null;
      if (parent != null) {
        grandParent = StringUtils.hasText(parent.getParent()) ?
          util.find(nodes, parent.getParent()) : null;
      }
      switch (op) {
        case "left":
          if (parent != null) {
            List<Node> brs = grandParent != null ? grandParent.getChildren() : nodes;
            parent.getChildren().remove(curr);
            int i = brs.indexOf(parent);
            brs.add(i + 1, curr);
          }
          break;
        case "right":
          List<Node> brs = parent == null ? nodes : parent.getChildren();
          int i = brs.indexOf(curr);
          if (i > 0) {
            brs.remove(curr);
            brs.get(i - 1).getChildren().add(curr);
          }
          break;
        case "up":
          brs = parent == null ? nodes : parent.getChildren();
          i = brs.indexOf(curr);
          if (i > 0) {
            Collections.swap(brs, i - 1, i);
          }
          break;
        case "down":
          brs = parent == null ? nodes : parent.getChildren();
          i = brs.indexOf(curr);
          if (i < brs.size() - 1) {
            Collections.swap(brs, i, i + 1);
          }
          break;
        default:
          break;
      }
    }
    nodes.forEach(n -> util.walk(n, 0));
    return editTree(tree.getRoot(), tree.getRootDesc(), version, derivatives);
  }

  public WordDictTree addDerivative(String id, String word, String input, int version) {
    WordDictTree tree = treeMapper.findById(id);
    List<WordDictTree.Derivative> list = tree.getDerivatives();
    if (list.stream().noneMatch(v -> Objects.equals(v.getWord(), input))) {
      if (!StringUtils.hasText(word)) {
        tree.getDerivatives().add(new WordDictTree.Derivative(input, 0, tree.getVersion() + 1, true));
      } else {
        int j = -1;
        for (int i = 0; i < list.size(); i++) {
          if (list.get(i).getWord().equals(word)) {
            j = i;
          }
        }
        if (j >= 0) {
          int z = j;
          for (int k = j + 1; k < list.size(); k++) {
            if (list.get(k).getIndex() <= list.get(j).getIndex()) {
              break;
            }
            z = k;
          }
          tree.getDerivatives().add(z + 1, new WordDictTree.Derivative(input, list.get(j).getIndex() + 1, tree.getVersion() + 1, true));
        }
      }
      return editTree(tree.getRoot(), tree.getRootDesc(), version, tree.getDerivatives());
    }
    return null;
  }

  public List<WordDict> search(String q) {
    SqlBuilder sb = factory.create();
    SqlBuilder.Template template = sb.from(WordDict.class)
      .where(sb.field("id").contains(q))
      .select("id", "meaning", "loadState", "passed", "viewed", "sort", "loadTime")
      .template();
    return mapper.findByTemplate(template);
  }

  public List<WordDictDiff> differs(String word) {
    WordDict dict = mapper.findById(word);
    if (dict != null && dict.getDiffers() != null && !dict.getDiffers().isEmpty()) {
      List<WordDictDiff> ret = diffMapper.findByIds(new HashSet<>(dict.getDiffers()));
      ret.sort(Comparator.comparingInt(v -> dict.getDiffers().indexOf(v.getId())));
      return ret;
    }
    return new ArrayList<>();
  }

  public List<WordDictTree> trees(String root) {
    return treeMapper.byRoot(root);
  }

  public List<WordDictTree> findTree(String word) {
    return treeMapper.findTree("{\"word\":\"" + word + "\"}");
  }

  public void createTree(String word) {
    WordDict dict = mapper.findById(word);
    if (dict != null) {
      dict.getStruct().getParts().stream().filter(WordDict.Part::isRoot).forEach(part -> {
        String root = part.getPart();
        String desc = part.getMeaningTrans();
        WordDictTree tree = treeMapper.byRootAndDesc(root, desc);
        if (tree == null) {
          List<String> ws = fetchDerivatives(root, word, root);
          ws = ws.stream().distinct().sorted(Comparator.comparingInt(String::length)).collect(Collectors.toList());
          List<WordDictTree.Derivative> derivatives = WordDerivativesLoader.build(word, root, ws).stream()
            .map(v -> new WordDictTree.Derivative(v.getWord(), v.getIndex(), 1, false))
            .collect(Collectors.toList());
          editTree(root, desc, 0, derivatives);
        }
      });
    }
  }

  public WordDictTree mergeTree(String treeId, String word) {
    WordDictTree tree = treeMapper.findById(treeId);
    List<WordDictTree.Derivative> derivatives = tree.getDerivatives();
    List<String> ws = fetchDerivatives(tree.getRoot(), word);
    ws.removeIf(v -> derivatives.stream().anyMatch(d -> Objects.equals(d.getWord(), v)));
    if (ws.isEmpty()) {
      return tree;
    }
    ws.add(tree.getRoot());
    ws = ws.stream().distinct().sorted(Comparator.comparingInt(String::length)).collect(Collectors.toList());
    List<WordDict.Derivative> news = WordDerivativesLoader.build(word, tree.getRoot(), ws);
    List<WordDictTree.Derivative> ds = tree.getDerivatives();
    for (int i = news.size() - 1; i >= 1; i--) {
      WordDict.Derivative n = news.get(i);
      if (n.getIndex() == 0) {
        ds.add(new WordDictTree.Derivative(n.getWord(), 0, tree.getVersion() + 1, true));
      } else {
        ds.add(1, new WordDictTree.Derivative(n.getWord(), n.getIndex(), tree.getVersion() + 1, true));
      }
    }
    return editTree(tree.getRoot(), tree.getRootDesc(), tree.getVersion(), ds);
  }

  public void editTreeDesc(String treeId, String desc, int version) {
    SqlBuilder sb = factory.create();
    SqlBuilder.Template template = sb.from(WordDictTree.class)
      .where(sb.field("id").eq(treeId))
      .forUpdate()
      .template();
    WordDictTree tree = treeMapper.findOneByTemplate(template);
    if (tree.getVersion() != version) {
      throw new ConstraintException("");
    }
    tree.setRootDesc(desc);
    treeMapper.update(tree);
  }

  private WordDictTree editTree(String root,
                                String desc,
                                int version,
                                List<WordDictTree.Derivative> derivatives) {
    WordDictTree tree = treeMapper.byRootAndDesc(root, desc);
    if (tree != null) {
      if (version != tree.getVersion()) {
        return tree;
      }
      SqlBuilder sb = factory.create();
      SqlBuilder.Template template = sb.from(WordDictTree.class)
        .where(sb.field("id").eq(tree.getId()))
        .forUpdate()
        .template();
      tree = treeMapper.findOneByTemplate(template);
      tree.setDerivatives(derivatives);
      tree.setVersion(tree.getVersion() + 1);
      treeMapper.update(tree);
      return tree;
    } else {
      tree = new WordDictTree();
      tree.setId(IdGenerator.next());
      tree.setRoot(root);
      tree.setRootDesc(desc);
      tree.setVersion(1);
      tree.setDerivatives(derivatives);
      treeMapper.insert(tree);
    }
    return tree;
  }

  @SuppressWarnings("Duplicates")
  private List<String> fetchDerivatives(String root, String... words) {
    List<String> ws = new ArrayList<>();
    Arrays.asList(words).forEach(w -> {
      ws.add(w);
      WordXxEnSpider.fetchDerivative(w, ws::addAll);
      WordHcSpider.fetchDerivative(w, root, ws::addAll);
      WordJsSpider.fetchDerivative(w, root, ws::addAll);
      WordXdfSpider.fetchDerivative(w, root, ws::addAll);
    });
    List<String> _ws = ws.stream()
      .flatMap(v -> Arrays.stream(v.split("/")))
      .map(String::toLowerCase)
      .distinct()
      .collect(Collectors.toList());
    _ws.forEach(w -> {
      if (!Arrays.asList(words).contains(w)) {
        WordXxEnSpider.fetchDerivative(w, ws::addAll);
        WordHcSpider.fetchDerivative(w, root, ws::addAll);
        WordJsSpider.fetchDerivative(w, root, ws::addAll);
        WordXdfSpider.fetchDerivative(w, root, ws::addAll);
      }
    });
    List<String> ret = ws.stream()
      .flatMap(v -> Arrays.stream(v.split("/")))
      .map(String::toLowerCase)
      .distinct()
      .collect(Collectors.toList());
    ret.removeIf(v -> {
      if (Arrays.asList(words).contains(v)) {
        return false;
      }
      boolean invalid = v.contains(" ") || v.contains("-") || v.contains("'");
      if (invalid) {
        return true;
      }
      WordDict d = mapper.findById(v);
      if (d != null) {
        return false;
      }
      WordLoaderAffix a = affixMapper.findById(v);
      if (a != null) {
        return false;
      }
      return !has(v);
    });
    return ret;
  }

  private boolean has(String word) {
    WordLoaderExist w = existMapper.findById(word);
    if (w != null) {
      return w.isHas();
    }
    boolean has = WordXxEnSpider.has(word);
    if (has) {
      has = WordYdSpider.has(word);
    }
    if (has) {
      try {
        Document node = WordDictLoader.fetchDocument("https://www.oxfordlearnersdictionaries.com/definition/english/" + word + "_1?q=" + word);
        has = XPaths.of(node, "//div[@id='didyoumean']").asArray().isEmpty();
      } catch (Throwable ex) {
        has = false;
      }
    }
    existMapper.insert(new WordLoaderExist(word, has));
    return has;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Node {
    private String word;
    private int version;
    private boolean merged;
    private String parent;
    private List<Node> children;
    private int index;
  }
}