package com.github.sun.word.spider;

import com.github.sun.spider.spi.XPaths;
import com.github.sun.word.WordDict;
import com.github.sun.word.WordDictLoader;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@Service
public class WordXdfSpider {
  public static void fetchDerivative(WordDict dict, Consumer<Set<String>> func) {
    Set<String> words = new LinkedHashSet<>();
    Document node = WordDictLoader.fetchDocument("https://www.koolearn.com/dict/search/index?keywords=" + dict.getId());
    List<Node> arr = XPaths.of(node, "//div[@class='retrieve']/div").asArray();
    int j = -1;
    for (int i = 0; i < arr.size(); i++) {
      if (arr.get(i).getTextContent().contains("同根词")) {
        j = i + 1;
        break;
      }
    }
    if (j > 0) {
      Node div = arr.get(j);
      XPaths.of(div, "./a").asArray().forEach(a -> {
        String name = StringEscapeUtils.unescapeHtml4(a.getTextContent()).trim();
        if (name.split(" ").length == 1) {
          words.add(name);
        }
      });
    }
    func.accept(words);
  }
}