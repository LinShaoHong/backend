package com.github.sun.word.loader;

import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.word.WordDict;
import com.github.sun.word.spider.WordJsSpider;
import com.github.sun.word.spider.WordYdSpider;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RefreshScope
@Service("meaning")
public class WordMeaningLoader extends WordBasicLoader {
  @Override
  public void load(String word, JSON.Valuer attr, int userId) {
    retry(word, userId, dict -> {
      Set<String> set = WordYdSpider.fetchMeaning(dict);
      set.addAll(WordJsSpider.fetchMeaning(dict));
      String ms = set.stream().map(v -> {
        if ("nouns".equals(v)) {
          return "名词";
        } else if ("verbs".equals(v)) {
          return "动词";
        } else if ("adjectives".equals(v)) {
          return "形容词";
        } else if ("adverbs".equals(v)) {
          return "副词";
        }
        return null;
      }).filter(Objects::nonNull).collect(Collectors.joining("、"));
      String q = loadQ("cues/释义.md");
      q = q.replace("$word", word).replace("$scope", ms);
      JSON.Valuer valuer = JSON.newValuer(parse(assistant.chat(apiKey, model, q)));
      dict.setMeaning(WordDict.TranslatedMeaning.builder()
        .nouns(valuer.get("translated_meanings").get("nouns").asText(""))
        .verbs(valuer.get("translated_meanings").get("verbs").asText(""))
        .adjectives(valuer.get("translated_meanings").get("adjectives").asText(""))
        .adverbs(valuer.get("translated_meanings").get("adverbs").asText(""))
        .build());
    }, "meaning");
  }
}