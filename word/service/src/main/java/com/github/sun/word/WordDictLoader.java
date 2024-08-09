package com.github.sun.word;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.sun.foundation.ai.Assistant;
import com.github.sun.foundation.boot.Scanner;
import com.github.sun.foundation.boot.utility.Dates;
import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.foundation.boot.utility.Reflections;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.spider.Fetcher;
import com.github.sun.spider.spi.JSoupFetcher;
import com.github.sun.word.loader.WordBasicLoader;
import com.github.sun.word.loader.WordDerivativesLoader;
import com.github.sun.word.loader.WordStructLoader;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RefreshScope
public class WordDictLoader {
  private static final Fetcher fetcher = new JSoupFetcher();
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
  private WordCheckMapper checkMapper;
  @Resource
  private WordCodeMapper codeMapper;
  @Resource
  private WordAffixMapper affixMapper;
  @Resource
  private WordStructLoader structLoader;
  @Resource
  private WordDerivativesLoader derivativesLoader;

  public String chat(String q) {
    return assistant.chat(apiKey, model, q);
  }

  public void loadAll(String words, int userId) {
    for (String word : words.split(",")) {
      WordBasicLoader.init(word, userId);
      Scanner.getClassesWithInterface(WordLoader.class)
        .stream()
        .filter(Scanner.ClassTag::isImplementClass)
        .filter(v -> v.runtimeClass() != WordStructLoader.class &&
          v.runtimeClass() != WordDerivativesLoader.class)
        .forEach(loader -> executor.submit(() -> loader.getInstance().load(word, userId)));
      executor.submit(() -> {
        structLoader.load(word, userId);
        derivativesLoader.load(word, userId);
      });
    }
  }

  public void loadPart(String word, String part, JsonNode attr, int userId) {
    WordBasicLoader.init(word, userId);
    mapper.loading(word, "'$." + part + "Loading'");
    Scanner.getClassesWithInterface(WordLoader.class)
      .stream().filter(v -> v.isImplementClass() &&
        v.runtimeClass().getAnnotation(Service.class).value().equals(part))
      .findFirst()
      .ifPresent(loader -> executor.submit(() -> loader.getInstance().load(word, attr == null ? null : JSON.newValuer(attr), userId)));
  }

  @Transactional
  public void removePart(String word, String part, String path, int userId) {
    WordDict dict = WordBasicLoader.init(word, userId);
    switch (part) {
      case "meaning":
        if (StringUtils.hasText(path)) {
          Reflections.setValue(dict.getMeaning(), path, "");
        } else {
          dict.setMeaning(new WordDict.TranslatedMeaning());
        }
        break;
      case "struct":
        if (!StringUtils.hasText(path)) {
          dict.setStruct(null);
        }
        break;
      case "inflection":
        if (StringUtils.hasText(path)) {
          Reflections.setValue(dict.getInflection(), path, Collections.emptyList());
        } else {
          dict.setInflection(new WordDict.Inflection());
        }
        break;
      case "phrases":
        if (StringUtils.hasText(path)) {
          dict.getPhrases().removeIf(d -> Objects.equals(d.getEn(), path));
        } else {
          dict.setDerivatives(Collections.emptyList());
        }
        break;
      case "derivatives":
        if (StringUtils.hasText(path)) {
          if (path.endsWith(":sub")) {
            String _path = path.substring(0, path.length() - 4);
            dict.getDerivatives().removeIf(d -> d.getWord().contains(_path));
          } else {
            dict.getDerivatives().removeIf(d -> d.getWord().equals(path));
          }
        } else {
          dict.setDerivatives(Collections.emptyList());
        }
        WordDerivativesLoader.rebuild(dict);
        break;
      case "differs":
        if (StringUtils.hasText(path)) {
          dict.getDiffers().removeIf(d -> Objects.equals(d.getWord(), path));
        } else {
          dict.setDiffers(Collections.emptyList());
        }
        break;
      case "synonym":
        if (StringUtils.hasText(path)) {
          dict.getSynAnts().getSynonyms().removeIf(d -> Objects.equals(d, path));
        } else {
          dict.getSynAnts().setSynonyms(new ArrayList<>());
        }
        break;
      case "antonym":
        if (StringUtils.hasText(path)) {
          dict.getSynAnts().getAntonyms().removeIf(d -> Objects.equals(d, path));
        } else {
          dict.getSynAnts().setAntonyms(new ArrayList<>());
        }
        break;
      default:
        break;
    }
    dict.setPassed(false);
    mapper.update(dict);
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
      int total = mapper.countByDate(date);
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
  public WordCheck stat(String date, int userId) {
    WordCheck check = checkMapper.findById(date + ":" + userId);
    check.setTotal(mapper.countByDate(date));
    check.setPassed(mapper.countByPassed(date));
    check.setViewed(mapper.countByViewed(date));
    return check;
  }

  @Transactional
  public List<WordCheck> stats(int userId) {
    List<String> dates = checkMapper.dates();
    return dates.stream().map(date -> {
      String id = date + ":" + userId;
      WordCheck check = checkMapper.findById(id);
      if (check == null) {
        check = new WordCheck();
        check.setId(date + ":" + userId);
        check.setUserId(userId);
        check.setDate(date);
        check.setSort(1);
        checkMapper.replace(check);
      }
      check.setTotal(mapper.countByDate(date));
      check.setPassed(mapper.countByPassed(date));
      check.setViewed(mapper.countByViewed(date));
      return check;
    }).collect(Collectors.toList());
  }

  @Transactional
  public WordDict dict(String date, Integer sort, int userId) {
    WordCheck check = null;
    List<WordCheck> all = stats(userId);
    if (!StringUtils.hasText(date)) {
      check = all.stream().filter(WordCheck::isCurr).findFirst().orElse(null);
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
    WordAffix affix = affixMapper.findById(word);
    return affix == null ? "" : affix.getRoot();
  }

  public WordAffix affix(String word) {
    return affixMapper.byId(word);
  }

  public static synchronized Document fetchDocument(String url) {
    return documents.get(url, u -> {
      try {
        String html = fetcher.fetch(url);
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
      .update().set("meaning", meaning)
      .template();
    mapper.updateByTemplate(template);
  }

  public void editStruct(String id, WordDict.Struct struct) {
    struct.getParts().removeIf(v -> !StringUtils.hasText(v.getPart()));
    SqlBuilder sb = factory.create();
    SqlBuilder.Template template = sb.from(WordDict.class)
      .where(sb.field("id").eq(id))
      .update().set("struct", struct)
      .template();
    mapper.updateByTemplate(template);
  }

  public void moveDerivative(String id, String word, String op) {
    WordDict dict = mapper.findById(id);
    if (dict != null) {
      List<WordDict.Derivative> list = dict.getDerivatives();
      List<WordDict.Derivative> derivatives = new ArrayList<>();
      class Util {
        public List<Node> make(String parent, int pos, int index) {
          List<Node> cs = new ArrayList<>();
          for (int i = pos; i < list.size(); i++) {
            WordDict.Derivative v = list.get(i);
            if (v.getIndex() < index) {
              break;
            }
            if (v.getIndex() == index) {
              Node node = new Node();
              node.setWord(v.getWord());
              node.setParent(parent);
              node.setChildren(make(node.getWord(), i + 1, v.getIndex() + 1));
              cs.add(node);
            }
          }
          return cs;
        }

        public void walk(Node node, int index) {
          derivatives.add(new WordDict.Derivative(node.getWord(), index));
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
      SqlBuilder sb = factory.create();
      SqlBuilder.Template template = sb.from(WordDict.class)
        .where(sb.field("id").eq(id))
        .update().set("derivatives", derivatives)
        .template();
      mapper.updateByTemplate(template);
    }
  }

  public void addDerivative(String id, String word, String input) {
    WordDict dict = mapper.findById(id);
    if (dict != null) {
      List<WordDict.Derivative> list = dict.getDerivatives();
      if (list.stream().noneMatch(v -> Objects.equals(v.getWord(), input))) {
        if (!StringUtils.hasText(word)) {
          dict.getDerivatives().add(new WordDict.Derivative(input, 0));
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
            dict.getDerivatives().add(z + 1, new WordDict.Derivative(input, list.get(j).getIndex() + 1));
          }
        }
        SqlBuilder sb = factory.create();
        SqlBuilder.Template template = sb.from(WordDict.class)
          .where(sb.field("id").eq(id))
          .update().set("derivatives", dict.getDerivatives())
          .template();
        mapper.updateByTemplate(template);
      }
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Node {
    private String word;
    private String parent;
    private List<Node> children;
    private int index;
  }
}