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
    int i = 0;
    for (; i < arr.size(); i++) {
      if (arr.get(i).getTextContent().contains("同根词")) {
        break;
      }
    }
    Node div = arr.get(i + 1);
    XPaths.of(div, "./a").asArray().forEach(a -> {
      String name = StringEscapeUtils.unescapeHtml4(a.getTextContent()).trim();
      if (name.split(" ").length == 1) {
        words.add(name);
      }
    });
    func.accept(words);
  }
}