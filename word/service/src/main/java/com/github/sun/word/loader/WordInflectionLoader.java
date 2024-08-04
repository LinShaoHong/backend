package com.github.sun.word.loader;

import com.github.sun.word.spider.WordYdSpider;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

@RefreshScope
@Service("inflection")
public class WordInflectionLoader extends WordBasicLoader {
  @Override
  public void load(String word, int userId) {
    retry(word, userId, dict -> {
      try {
        WordYdSpider.fetchPhonetic(dict);
        WordYdSpider.fetchInflection(dict);
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    }, "ukPhonetic", "usPhonetic", "inflection");
  }
}