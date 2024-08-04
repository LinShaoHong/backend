package com.github.sun.word.spider;

import com.github.sun.spider.spi.XPaths;
import com.github.sun.word.WordDict;
import com.github.sun.word.WordDictLoader;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@Service
public class WordYdSpider {
  public static void main(String[] args) {
    try {
      WordDict dict = new WordDict();
      dict.setId("abstract");
      WordHcSpider.fetchSynAnts(dict, synAnt -> {
        System.out.println(synAnt.getSynonyms());
        System.out.println("-------------------");
        System.out.println(synAnt.getAntonyms());
      });
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void fetchPhonetic(WordDict dict) {
    Document node = WordDictLoader.fetchDocument("https://dict.youdao.com/result?lang=en&word=" + dict.getId());
    List<Node> arr = XPaths.of(node, "//div[@class='per-phone']").asArray();
    String uk = XPaths.of(arr.get(0), ".//span[@class='phonetic']/text()").asText();
    String us = XPaths.of(arr.get(1), ".//span[@class='phonetic']/text()").asText();
    uk = StringEscapeUtils.unescapeHtml4(uk);
    us = StringEscapeUtils.unescapeHtml4(us);
    dict.setUkPhonetic(uk);
    dict.setUsPhonetic(us);
  }

  public static void fetchInflection(WordDict dict) {
    Document node = WordDictLoader.fetchDocument("https://dict.youdao.com/result?lang=en&word=" + dict.getId());
    WordDict.Inflection inflection = new WordDict.Inflection();
    List<Node> arr = XPaths.of(node, "//li[@class='word-wfs-cell-less']").asArray();
    arr.forEach(n -> {
      String name = XPaths.of(n, ".//span[@class='wfs-name']").asText();
      String words = XPaths.of(n, ".//span[@class='transformation']").asText();
      name = StringEscapeUtils.unescapeHtml4(name);
      words = StringEscapeUtils.unescapeHtml4(words);
      switch (name) {
        case "复数":
          inflection.setPlural(Collections.singletonList(words));
          break;
        case "第三人称单数":
          inflection.setThirdPresent(Collections.singletonList(words));
          break;
        case "现在分词":
          inflection.setProgressive(Collections.singletonList(words));
          break;
        case "过去式":
          inflection.setPast(Collections.singletonList(words));
          break;
        case "过去分词":
          inflection.setPerfect(Collections.singletonList(words));
          break;
        case "比较级":
          inflection.setComparative(Collections.singletonList(words));
          break;
        case "最高级":
          inflection.setSuperlative(Collections.singletonList(words));
          break;
        default:
          break;
      }
    });
    dict.setInflection(inflection);
  }

  public static void fetchPhrase(WordDict dict, Consumer<WordDict.Phrase> func) {
    try {
      Document node = WordDictLoader.fetchDocument("https://dict.youdao.com/result?lang=en&word=" + dict.getId());
      List<Node> arr = XPaths.of(node, "//div[@class='webPhrase']//li[@class='mcols-layout']").asArray();
      arr.forEach(v -> {
        String name = XPaths.of(v, ".//a[@class='point']").asText().trim();
        String desc = XPaths.of(v, ".//p").as().getTextContent().trim();
        name = StringEscapeUtils.unescapeHtml4(name);
        desc = StringEscapeUtils.unescapeHtml4(desc);
        if (StringUtils.hasText(name) && StringUtils.hasText(desc)) {
          func.accept(new WordDict.Phrase(name, desc));
        }
      });
    } catch (Exception ex) {
    }
  }
}