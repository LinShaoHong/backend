package com.github.sun.word.spider;

import com.github.sun.spider.Fetcher;
import com.github.sun.spider.spi.JSoupFetcher;
import com.github.sun.spider.spi.XPaths;
import org.apache.commons.text.StringEscapeUtils;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.List;

@Service
public class WordHcSpider {
  private static final Fetcher fetcher = new JSoupFetcher();
  private static final HtmlCleaner hc = new HtmlCleaner();

  public static void fetchPhrase(String word) {
    try {
      String html = fetcher.fetch("https://dict.cn/search?q=" + word);
      Document node = new DomSerializer(new CleanerProperties()).createDOM(hc.clean(html));
      List<Node> arr = XPaths.of(node, "//div[@class='layout coll']//li").asArray();
      arr.addAll(XPaths.of(node, "//div[@class='layout anno']//li").asArray());
      arr.forEach(v -> {
        String name = XPaths.of(v, "./a/text()").asText().trim();
        String desc = v.getTextContent().trim().substring(name.length()).trim();
        name = StringEscapeUtils.unescapeHtml4(name);
        desc = StringEscapeUtils.unescapeHtml4(desc);
        System.out.println(name + ": " + desc);
      });
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}