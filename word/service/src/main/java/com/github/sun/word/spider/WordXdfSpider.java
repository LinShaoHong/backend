package com.github.sun.word.spider;

import com.github.sun.spider.spi.XPaths;
import com.github.sun.word.WordDictLoader;
import com.github.sun.word.WordTagMapper;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.annotation.Resource;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@Service
public class WordXdfSpider {
  @Resource
  private WordTagMapper mapper;

  public static void fetchDerivative(String word, Consumer<Set<String>> func) {
    try {
      Set<String> words = new LinkedHashSet<>();
      Document node = WordDictLoader.fetchDocument("https://www.koolearn.com/dict/search/index?keywords=" + word);
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
    } catch (Throwable ex) {
      // do nothing
    }
  }

  public void fetchWords(String uri, int start, int end) {
    for (int i = start; i <= end; i++) {
      String url = String.format(uri, i);
      Document node = WordDictLoader.fetchDocument(url);
      List<Node> arr = XPaths.of(node, "//a[@class='word']").asArray();
      arr.forEach(a -> {
        System.out.println(a.getTextContent());
      });
    }
  }
}