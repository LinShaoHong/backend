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
@Service("synAnts")
public class WordSynAntsLoader extends WordBasicLoader {
  @Override
  public void load(String word, int userId) {
    retry(word, userId, dict -> {
      String q = loadQ("cues/近反义词.md");
      String resp = assistant.chat(apiKey, model, "直接列出单词'" + word + "'的所有英文近义词和反义词。要求不要包含短语，单词首字母小写。");
      JSON.Valuer valuer = JSON.newValuer(parse(assistant.chat(apiKey, model, resp + "\n\n" + q)));
      WordDict.SynAnt synAnt = new WordDict.SynAnt();
      Set<String> synonyms = new LinkedHashSet<>();
      Set<String> antonyms = new LinkedHashSet<>();

      valuer.get("synonyms").asArray().forEach(f -> synonyms.add(f.get("word").asText()));
      synonyms.removeIf(v -> Objects.equals(v, word));
      synAnt.setSynonyms(new ArrayList<>(synonyms));

      valuer.get("antonyms").asArray().forEach(f -> antonyms.add(f.get("word").asText()));
      antonyms.removeIf(v -> Objects.equals(v, word));
      synAnt.setAntonyms(new ArrayList<>(antonyms));

      dict.setSynAnts(synAnt);
    }, "synAnts");
  }
}