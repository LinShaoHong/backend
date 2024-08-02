package com.github.sun.word.spider;

import com.github.sun.spider.Fetcher;
import com.github.sun.spider.spi.JSoupFetcher;
import com.github.sun.spider.spi.XPaths;
import com.github.sun.word.WordDict;
import org.apache.commons.text.StringEscapeUtils;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.Collections;
import java.util.List;

@Service
public class WordYdSpider {
  private static final Fetcher fetcher = new JSoupFetcher();
  private static final HtmlCleaner hc = new HtmlCleaner();

  public static void main(String[] args) {
    try {
      String html = fetcher.fetch("https://dict.youdao.com/result?lang=en&word=abstract");
      Document node = new DomSerializer(new CleanerProperties()).createDOM(hc.clean(html));
//      WordXxEnSpider.fetchPhrase("starve");
//      WordHcSpider.fetchPhrase("starve");
      fetchPhrase(node, null);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  public static void fetchPhonetic(Document node, WordDict dict) {
    List<Node> arr = XPaths.of(node, "//div[@class='per-phone']").asArray();
    String uk = XPaths.of(arr.get(0), ".//span[@class='phonetic']/text()").asText();
    String us = XPaths.of(arr.get(1), ".//span[@class='phonetic']/text()").asText();
    uk = StringEscapeUtils.unescapeHtml4(uk);
    us = StringEscapeUtils.unescapeHtml4(us);
    dict.setUkPhonetic(uk);
    dict.setUsPhonetic(us);
  }

  public static void fetchInflection(Document node, WordDict dict) {
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

  public static void fetchPhrase(Document node, WordDict dict) {
    List<Node> arr = XPaths.of(node, "//div[@class='webPhrase']//li[@class='mcols-layout']").asArray();
    arr.forEach(v -> {
      String name = XPaths.of(v, ".//a[@class='point']").asText().trim();
      String desc = XPaths.of(v, ".//p").as().getTextContent().trim();
      name = StringEscapeUtils.unescapeHtml4(name);
      desc = StringEscapeUtils.unescapeHtml4(desc);
      System.out.println(name + ": " + desc);
    });
  }
}