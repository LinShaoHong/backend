package com.github.sun.word.loader;

import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.word.WordDict;
import com.github.sun.word.spider.WordJsSpider;
import com.github.sun.word.spider.WordYdSpider;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RefreshScope
@Service("examples")
public class WordExamplesLoader extends WordBasicLoader {
  @Override
  public void load(String word, JSON.Valuer attr, int userId) {
    retry(word, userId, dict -> {
      Set<String> set = WordYdSpider.fetchMeaning(dict);
      set.addAll(WordJsSpider.fetchMeaning(dict));
      String ms = set.stream().map(v -> {
        if ("nouns".equals(v)) {
          return "名词形式";
        } else if ("verbs".equals(v)) {
          return "动词形式";
        } else if ("adjectives".equals(v)) {
          return "形容词形式";
        } else if ("adverbs".equals(v)) {
          return "副词形式";
        }
        return "";
      }).collect(Collectors.joining("、"));

      String q = loadQ("cues/释义例句.md");
      q = q.replace("$ms", ms);
      JSON.Valuer valuer = JSON.newValuer(parse(assistant.chat(apiKey, model, q, word)));
      dict.setMeaning(WordDict.TranslatedMeaning.builder()
        .nouns(valuer.get("translated_meanings").get("nouns").asText())
        .verbs(valuer.get("translated_meanings").get("verbs").asText())
        .adjectives(valuer.get("translated_meanings").get("adjectives").asText())
        .adverbs(valuer.get("translated_meanings").get("adverbs").asText())
        .build());
      List<WordDict.ExampleSentence> examples = new ArrayList<>();
      valuer.get("example_sentences").asArray().forEach(e -> {
        String sentence = e.get("sentence").asText();
        String translation = e.get("translation").asText();
        examples.add(new WordDict.ExampleSentence(sentence, translation));
      });
      dict.setExamples(examples);
    }, "meaning", "examples");
  }
}