package com.github.sun.word.spider;

import com.github.sun.spider.XPaths;
import com.github.sun.word.WordDict;
import com.github.sun.word.WordDictLoader;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@Service
public class WordHcSpider {
  public static void fetchDerivative(String word, String root, Consumer<Set<String>> func) {
    try {
      Set<String> words = new LinkedHashSet<>();
      Document node = WordDictLoader.fetchDocument("https://dict.cn/search?q=" + word);
      List<Node> arr = XPaths.of(node, "//div[@class='layout derive']//li").asArray();
      arr.forEach(v -> {
        String name = StringEscapeUtils.unescapeHtml4(v.getTextContent());
        for (String n : name.split("‖")) {
          words.add(n.split(" ")[0]);
        }
      });
      arr = XPaths.of(node, "//div[@class='layout nwd']//a").asArray();
      arr.forEach(v -> {
        String name = StringEscapeUtils.unescapeHtml4(v.getTextContent());
        name = name.trim().split(" ")[0];
        if (name.contains(root)) {
          words.add(name);
        }
      });
      func.accept(words);
    } catch (Throwable ex) {
      //do nothing
    }
  }

  public static void fetchSynAnts(WordDict dict, Consumer<WordDict.SynAnt> func) {
    Document node = WordDictLoader.fetchDocument("https://dict.cn/search?q=" + dict.getId());
    List<Node> arr = XPaths.of(node, "//div[@class='layout nfo']/ul").asArray();
    WordDict.SynAnt synAnt = new WordDict.SynAnt();
    synAnt.setSynonyms(new ArrayList<>());
    synAnt.setAntonyms(new ArrayList<>());
    if (!arr.isEmpty()) {
      List<Node> ns = XPaths.of(arr.get(0), "./li//a").asArray();
      ns.forEach(n -> synAnt.getSynonyms().add(n.getTextContent().trim()));
      if (arr.size() > 1) {
        ns = XPaths.of(arr.get(1), "./li//a").asArray();
        ns.forEach(n -> synAnt.getAntonyms().add(n.getTextContent().trim()));
      }
    }
    func.accept(synAnt);
  }
}
