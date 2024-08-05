package com.github.sun.word.loader;

import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.word.WordDict;
import com.github.sun.word.spider.WordHcSpider;
import com.github.sun.word.spider.WordXxEnSpider;
import com.github.sun.word.spider.WordYdSpider;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RefreshScope
@Service("phrases")
public class WordPhrasesLoader extends WordBasicLoader {
  @Override
  public void load(String word, JSON.Valuer attr, int userId) {
    retry(word, userId, dict -> {
      Set<WordDict.Phrase> phrases = new LinkedHashSet<>();
      String q = loadQ("cues/短语词组.md");
      try {
        String resp = assistant.chat(apiKey, model, q.replace("$word", word));
        JSON.Valuer valuer = JSON.newValuer(parse(resp));
        valuer.asArray().forEach(a -> phrases.add(new WordDict.Phrase(a.get("phrase").asText(), a.get("translation").asText())));
      } catch (Throwable ex) {
        //do nothing
      }

      List<WordDict.Phrase> ydPhrases = new ArrayList<>();
      WordYdSpider.fetchPhrase(dict, ydPhrases::add);
      if (ydPhrases.size() > 5) {
        ydPhrases = ydPhrases.subList(0, 5);
      }
      phrases.addAll(ydPhrases);

      List<WordDict.Phrase> hcPhrases = new ArrayList<>();
      WordHcSpider.fetchPhrase(dict, hcPhrases::add);
      if (hcPhrases.size() > 5) {
        hcPhrases = hcPhrases.subList(0, 5);
      }
      phrases.addAll(hcPhrases);

      List<WordDict.Phrase> xxEnPhrases = new ArrayList<>();
      WordXxEnSpider.fetchPhrase(dict, xxEnPhrases::add);
      if (xxEnPhrases.size() > 5) {
        xxEnPhrases = xxEnPhrases.subList(0, 5);
      }
      phrases.addAll(xxEnPhrases);

      dict.setPhrases(new ArrayList<>(phrases));
    }, "phrases");
  }
}