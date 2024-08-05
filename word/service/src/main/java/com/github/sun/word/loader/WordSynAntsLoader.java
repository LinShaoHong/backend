package com.github.sun.word.loader;

import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.word.WordDict;
import com.github.sun.word.spider.WordHcSpider;
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
  public void load(String word, JSON.Valuer attr, int userId) {
    retry(word, userId, dict -> {
      Set<String> synonyms = new LinkedHashSet<>();
      Set<String> antonyms = new LinkedHashSet<>();

      String q = loadQ("cues/近反义词.md");
      String resp = assistant.chat(apiKey, model, q.replace("$input", "直接列出单词'" + word + "'的英文近义词和反义词。要求每项最多5个，不要包含短语，单词首字母小写。"));
      JSON.Valuer valuer = JSON.newValuer(parse(resp));

      WordDict.SynAnt synAnt = new WordDict.SynAnt();

      valuer.get("synonyms").asArray().forEach(f -> synonyms.add(f.get("word").asText()));
      valuer.get("antonyms").asArray().forEach(f -> antonyms.add(f.get("word").asText()));

      WordHcSpider.fetchSynAnts(dict, vs -> {
        if (vs.getSynonyms().size() > 5) {
          synonyms.addAll(vs.getSynonyms().subList(0, 5));
        } else {
          synonyms.addAll(vs.getSynonyms());
        }
        if (vs.getAntonyms().size() > 5) {
          antonyms.addAll(vs.getAntonyms().subList(0, 5));
        } else {
          antonyms.addAll(vs.getAntonyms());
        }
      });

      synonyms.removeIf(v -> Objects.equals(v, word));
      synAnt.setSynonyms(new ArrayList<>(synonyms));

      antonyms.removeIf(v -> Objects.equals(v, word));
      synAnt.setAntonyms(new ArrayList<>(antonyms));

      dict.setSynAnts(synAnt);
    }, "synAnts");
  }
}