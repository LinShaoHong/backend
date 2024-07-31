package com.github.sun.word.loader;

import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.word.WordDict;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@RefreshScope
@Service("inflection")
public class WordInflectionLoader extends WordBasicLoader {
  @Override
  public void load(String word, int userId) {
    retry(word, userId, dict -> {
      String q = loadQ("cues/派生词.md");
      String resp = assistant.chat(apiKey, model, "直接列出单词'" + word + "'的单复数、进行时、过去时、完成时、第三人称、比较级和最高级的变形。" +
        "要求只需给出该单词的相关变形，不要分析其他单词，不要给出短语和词组形式，不要给出非标准给非常规形式，单词本身没有比较级时不要给出，中文不要给出。");
      System.out.println(resp);
      JSON.Valuer valuer = JSON.newValuer(parse(assistant.chat(apiKey, model, resp + "\n\n" + q)));
      WordDict.Inflection inflection = new WordDict.Inflection();

      Set<String> plural = new LinkedHashSet<>();
      valuer.get("plural").asArray().forEach(s -> plural.add(s.get("word").asText()));
      plural.removeIf(v -> Objects.equals(v, word) || v.isEmpty());
      inflection.setPlural(new ArrayList<>(plural));

      Set<String> progressive = new LinkedHashSet<>();
      valuer.get("progressive").asArray().forEach(s -> progressive.add(s.get("word").asText()));
      progressive.removeIf(v -> Objects.equals(v, word) || v.isEmpty());
      inflection.setProgressive(new ArrayList<>(progressive));

      Set<String> perfect = new LinkedHashSet<>();
      valuer.get("perfect").asArray().forEach(s -> perfect.add(s.get("word").asText()));
      perfect.removeIf(v -> Objects.equals(v, word) || v.isEmpty());
      inflection.setPerfect(new ArrayList<>(perfect));

      Set<String> past = new LinkedHashSet<>();
      valuer.get("past").asArray().forEach(s -> past.add(s.get("word").asText()));
      past.removeIf(v -> Objects.equals(v, word) || v.isEmpty());
      inflection.setPast(new ArrayList<>(past));

      Set<String> thirdPresent = new LinkedHashSet<>();
      valuer.get("3rd_person").asArray().forEach(s -> thirdPresent.add(s.get("word").asText()));
      thirdPresent.removeIf(v -> Objects.equals(v, word) || v.isEmpty());
      inflection.setThirdPresent(new ArrayList<>(thirdPresent));

      Set<String> comparative = new LinkedHashSet<>();
      valuer.get("comparative").asArray().forEach(s -> comparative.add(s.get("word").asText()));
      comparative.removeIf(v -> Objects.equals(v, word) || v.isEmpty());
      inflection.setComparative(new ArrayList<>(comparative));

      Set<String> superlative = new LinkedHashSet<>();
      valuer.get("superlative").asArray().forEach(s -> superlative.add(s.get("word").asText()));
      superlative.removeIf(v -> Objects.equals(v, word) || v.isEmpty());
      inflection.setSuperlative(new ArrayList<>(superlative));

      dict.setInflection(inflection);
    }, "inflection");
  }
}