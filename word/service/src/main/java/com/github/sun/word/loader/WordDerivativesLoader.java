package com.github.sun.word.loader;

import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.foundation.boot.utility.StringSorts;
import com.github.sun.word.WordAffix;
import com.github.sun.word.WordAffixMapper;
import com.github.sun.word.WordDict;
import com.github.sun.word.WordDictMapper;
import com.github.sun.word.spider.WordHcSpider;
import com.github.sun.word.spider.WordYdSpider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@RefreshScope
@Service("derivatives")
public class WordDerivativesLoader extends WordBasicLoader {
  @Resource
  private WordDictMapper dictMapper;
  @Resource
  private WordAffixMapper affixMapper;

  @Override
  public void load(String word, int userId) {
    retry(word, userId, dict -> {
      String root = dict.getStruct().getParts().stream()
        .filter(WordDict.Part::isRoot)
        .map(part -> {
          String w = part.getPart();
          w = w.replaceAll("-", "");
          if (w.contains("(")) {
            int i = w.indexOf("(");
            w = w.substring(0, i);
          }
          return w;
        }).collect(Collectors.joining());
      root = StringUtils.hasText(root) ? root : word;

      String q = loadQ("cues/派生树.md");
      List<String> words = new ArrayList<>();
      words.add(word);
      words.add(root);
      WordHcSpider.fetchDerivative(dict, words::addAll);

      boolean hasRoot = dict.getStruct().getParts().stream().anyMatch(WordDict.Part::isRoot);
      String resp;
      if (hasRoot) {
        words.addAll(affixMapper.byRoot(root));
        resp = assistant.chat(apiKey, model, "尽可能多的直接列出词根\"" + root + "\"的所有派生词");
        JSON.Valuer valuer = JSON.newValuer(parse(assistant.chat(apiKey, model, resp + "\n\n" + q)));
        for (JSON.Valuer v : valuer.asArray()) {
          words.add(v.get("word").asText());
        }
      }
      resp = assistant.chat(apiKey, model, "尽可能多的直接列出单词\"" + word + "\"的所有派生词");
      JSON.Valuer valuer = JSON.newValuer(parse(assistant.chat(apiKey, model, resp + "\n\n" + q)));
      for (JSON.Valuer v : valuer.asArray()) {
        words.add(v.get("word").asText());
      }
      List<String> _words = words.stream().distinct().collect(Collectors.toList());
      dict.setDerivatives(merge(dict, _words, root));
    }, "derivatives");
  }

  private List<WordDict.Derivative> merge(WordDict dict, List<String> words, String root) {
    clearInvalidate(words, root);
    String word = dict.getId();
    List<String> _words = words.stream().map(String::toLowerCase).distinct().collect(Collectors.toList());
    dict.getDerivatives().forEach(d -> _words.add(d.getWord()));
    List<String> ws = _words.stream().distinct().sorted(Comparator.comparingInt(String::length)).collect(Collectors.toList());
    ws.removeIf(v -> !v.toLowerCase().contains(root.toLowerCase()) || v.contains(" ") || v.contains("-") || v.contains("'s"));
    return build(word, ws);
  }

  private static List<WordDict.Derivative> build(String word, List<String> ws) {
    List<WordNode> nodes = ws.stream().map(w -> {
      WordNode node = new WordNode();
      node.setWord(w);
      for (int i = ws.size() - 1; i >= 0; i--) {
        String _word = ws.get(i);
        if (w.contains(_word) && !Objects.equals(w, _word)) {
          node.setParent(_word);
          break;
        }
      }
      return node;
    }).collect(Collectors.toList());

    List<WordDict.Derivative> derivatives = new ArrayList<>();
    if (nodes.size() > 1) {
      Map<String, List<WordNode>> map = nodes.stream()
        .filter(v -> v.getParent() != null)
        .collect(Collectors.groupingBy(WordNode::getParent));

      class Util {
        public WordNode make(WordNode root, int level) {
          List<WordNode> vs = map.getOrDefault(root.getWord(), Collections.emptyList());
          List<WordNode> children = vs.stream()
            .map(v -> make(v, level + 1))
            .collect(Collectors.toList());
          sort(children);
          root.setChildren(children);
          root.setIndex(level);
          root.setHas(root.getWord().contains(word) || children.stream().anyMatch(WordNode::isHas));
          return root;
        }

        public void walk(WordNode root) {
          derivatives.add(new WordDict.Derivative(root.getWord(), root.getIndex()));
          root.getChildren().forEach(this::walk);
        }
      }

      Util util = new Util();
      List<WordNode> trees = nodes.stream()
        .filter(v -> v.getParent() == null)
        .map(v -> util.make(v, 0))
        .collect(Collectors.toList());
      sort(trees);
      trees.forEach(util::walk);
    }
    return derivatives;
  }

  public static void rebuild(WordDict dict) {
    if (!CollectionUtils.isEmpty(dict.getDerivatives())) {
      List<String> words = dict.getDerivatives().stream()
        .map(WordDict.Derivative::getWord).collect(Collectors.toList());
      dict.setDerivatives(build(dict.getId(), words));
    }
  }

  private void clearInvalidate(List<String> words, String root) {
    words.removeIf(v -> {
      if (Objects.equals(v, root)) {
        return false;
      }
      boolean invalid = !v.toLowerCase().contains(root.toLowerCase()) || v.contains(" ") || v.contains("-") || v.contains("'s");
      if (invalid) {
        return true;
      }
      WordDict d = dictMapper.findById(v);
      if (d != null) {
        return false;
      }
      WordAffix a = affixMapper.findById(v);
      if (a != null) {
        return false;
      }
      WordDict in = new WordDict();
      in.setId(v);
      try {
        WordYdSpider.fetchPhonetic(in);
      } catch (Exception ex) {
        return true;
      }
      return false;
    });
  }

  private static void sort(List<WordNode> nodes) {
    List<String> has = nodes.stream().filter(WordNode::isHas).map(WordNode::getWord).collect(Collectors.toList());
    List<String> nos = nodes.stream().filter(v -> !v.isHas()).map(WordNode::getWord).collect(Collectors.toList());
    has = StringSorts.sort(has, 0.5);
    nos = StringSorts.sort(nos, 0.5);
    has.addAll(nos);
    List<String> _has = has;
    nodes.sort(Comparator.comparingInt(v -> _has.indexOf(v.getWord())));
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class WordNode {
    private String word;
    private String parent;
    private int index;
    private boolean has;
    private List<WordNode> children;
  }
}