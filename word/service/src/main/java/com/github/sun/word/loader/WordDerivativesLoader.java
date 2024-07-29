package com.github.sun.word.loader;

import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.word.WordDict;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@RefreshScope
@Service("derivatives")
public class WordDerivativesLoader extends WordBasicLoader {
  @Override
  public void load(String word) {
    retry(word, dict -> {
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
      String _root = root;

      String q = loadQ("cues/派生树.md");
      List<String> words = new ArrayList<>();
      words.add(word);
      words.add(root);

      String resp = assistant.chat(apiKey, model, "尽可能多的直接列出与单词\"" + word + "\"有关的所有派生词和词源");
      JSON.Valuer valuer = JSON.newValuer(parse(assistant.chat(apiKey, model, resp + "\n\n" + q)));
      for (JSON.Valuer v : valuer.asArray()) {
        words.add(v.get("word").asText());
      }

      words = words.stream().distinct().collect(Collectors.toList());
      words.sort(Comparator.comparingInt(String::length));
      words.removeIf(v -> !v.toLowerCase().contains(_root.toLowerCase()) || v.contains(" ") || v.contains("-") || v.contains("'s"));

      for (String w : words) {
        if (word.toLowerCase().contains(w.toLowerCase()) && !w.equalsIgnoreCase(word)) {
          resp = assistant.chat(apiKey, model, "尽可能多的直接列出与单词\"" + w + "\"有关的所有派生词和词源");
          valuer = JSON.newValuer(parse(assistant.chat(apiKey, model, resp + "\n\n" + q)));
          for (JSON.Valuer v : valuer.asArray()) {
            words.add(v.get("word").asText());
          }
          break;
        }
      }

      words.removeIf(v -> !v.toLowerCase().contains(_root.toLowerCase()) || v.contains(" ") || v.contains("-") || v.contains("'s"));
      List<String> _words = words.stream().map(String::toLowerCase).distinct().collect(Collectors.toList());
      dict.getDerivatives().forEach(d -> _words.add(d.getWord()));
      List<String> ws = _words.stream().distinct().sorted(Comparator.comparingInt(String::length)).collect(Collectors.toList());
      ws.removeIf(v -> !v.toLowerCase().contains(_root.toLowerCase()) || v.contains(" ") || v.contains("-") || v.contains("'s"));

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
            root.setChildren(children);
            root.setIndex(level);
            root.setHas(root.getWord().contains(word) || children.stream().anyMatch(WordNode::isHas));
            children.sort((o1, o2) -> o1.isHas() ? (o2.isHas() ? (o1.getWord().length() - o2.getWord().length()) : -Integer.MAX_VALUE) : (o1.getWord().length() - o2.getWord().length()));
            return root;
          }

          public void walk(WordNode root) {
            derivatives.add(new WordDict.Derivative(root.getWord(), root.getIndex()));
            root.getChildren().forEach(this::walk);
          }
        }

        Util util = new Util();
        WordNode node = util.make(nodes.get(0), 0);
        util.walk(node);
      }
      dict.setDerivatives(derivatives);
    }, "derivatives");
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