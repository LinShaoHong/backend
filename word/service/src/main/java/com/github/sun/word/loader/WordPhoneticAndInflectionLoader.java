package com.github.sun.word.loader;

import com.github.sun.spider.Fetcher;
import com.github.sun.spider.spi.JSoupFetcher;
import com.github.sun.word.spider.WordYdSpider;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

@RefreshScope
@Service("phoneticAndInflection")
public class WordPhoneticAndInflectionLoader extends WordBasicLoader {
  private static final Fetcher fetcher = new JSoupFetcher();
  private static final HtmlCleaner hc = new HtmlCleaner();

  @Override
  public void load(String word, int userId) {
    retry(word, userId, dict -> {
      try {
        String html = fetcher.fetch("https://dict.youdao.com/result?lang=en&word="+word);
        Document node = new DomSerializer(new CleanerProperties()).createDOM(hc.clean(html));
        WordYdSpider.fetchPhonetic(node, dict);
        WordYdSpider.fetchInflection(node, dict);
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    }, "ukPhonetic", "usPhonetic", "inflection");
  }
}