package com.github.sun.word.loader;

import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.word.WordDict;
import com.github.sun.word.spider.WordJsSpider;
import com.github.sun.word.spider.WordXxEnSpider;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RefreshScope
@Service("differs")
public class WordDiffersLoader extends WordBasicLoader {
  @Override
  public void load(String word, JSON.Valuer attr, int userId) {
    retry(word, userId, dict -> {
      Set<String> ws = new LinkedHashSet<>();
      ws.add(dict.getId());
      ws.addAll(WordJsSpider.fetchDiffs(dict));
      ws.addAll(WordXxEnSpider.fetchDiffs(dict));
      List<WordDict.Differ> differs = new ArrayList<>();
      if (ws.size() > 1) {
        String q = loadQ("cues/辨析.md");
        String w = String.join("、", ws);
        String resp = assistant.chat(apiKey, model,
          q.replace("$input", "直接精简地对单词" + w + "进行辨析，要求只要包含单词的强调概念，使用场景以及2个例句并附上中文翻译"));
        JSON.Valuer valuer = JSON.newValuer(parse(resp));
        valuer.asArray().forEach(a -> {
          WordDict.Differ differ = new WordDict.Differ();
          differ.setWord(a.get("word").asText());
          differ.setDefinition(a.get("emphasized_aspect_zh").asText(""));
          differ.setScenario(a.get("usage_scenario_zh").asText(""));
          List<WordDict.ExampleSentence> examples = new ArrayList<>();
          a.get("examples").asArray().forEach(e -> {
            WordDict.ExampleSentence example = new WordDict.ExampleSentence();
            example.setSentence(e.get("sentence").asText(""));
            example.setTranslation(e.get("translation").asText(""));
            examples.add(example);
          });
          differ.setExamples(examples);
          differs.add(differ);
        });
      }
      dict.setDiffers(differs);
    }, "differs");
  }
}