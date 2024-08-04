package com.github.sun.word.loader;

import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.word.WordDict;
import com.github.sun.word.spider.WordHcSpider;
import com.github.sun.word.spider.WordXxEnSpider;
import com.github.sun.word.spider.WordYdSpider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service("phrases")
public class WordPhrasesLoader extends WordBasicLoader {
  @Override
  public void load(String word, int userId) {
    retry(word, userId, dict -> {
      Set<WordDict.Phrase> phrases = new LinkedHashSet<>();
      String q = loadQ("cues/短语词组.md");
      String resp = assistant.chat(apiKey, model, "请直接列出与单词" + word + "相关的10个常用短语词组，并给出翻译");
      JSON.Valuer valuer = JSON.newValuer(parse(assistant.chat(apiKey, model, resp + "\n\n" + q)));
      valuer.asArray().forEach(a -> {
        phrases.add(new WordDict.Phrase(a.get("phrase").asText(), a.get("translation").asText()));
      });

      List<WordDict.Phrase> ydPhrases = new ArrayList<>();
      WordYdSpider.fetchPhrase(dict, ydPhrases::add);
      if (ydPhrases.size() > 10) {
        ydPhrases = ydPhrases.subList(0, 10);
      }
      phrases.addAll(ydPhrases);

      List<WordDict.Phrase> hcPhrases = new ArrayList<>();
      WordHcSpider.fetchPhrase(dict, hcPhrases::add);
      if (hcPhrases.size() > 10) {
        hcPhrases = hcPhrases.subList(0, 10);
      }
      phrases.addAll(hcPhrases);

      List<WordDict.Phrase> xxEnPhrases = new ArrayList<>();
      WordXxEnSpider.fetchPhrase(dict, xxEnPhrases::add);
      if (xxEnPhrases.size() > 10) {
        xxEnPhrases = xxEnPhrases.subList(0, 10);
      }
      phrases.addAll(xxEnPhrases);

      dict.setPhrases(new ArrayList<>(phrases));
    }, "phrases");
  }
}