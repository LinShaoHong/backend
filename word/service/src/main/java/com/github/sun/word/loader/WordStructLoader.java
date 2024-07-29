package com.github.sun.word.loader;

import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.word.WordDict;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("struct")
public class WordStructLoader extends WordBasicLoader {
  @Override
  public void load(String word) {
    retry(word, dict -> {
      String q = loadQ("cues/词根词缀.md");
      String resp = assistant.chat(apiKey, model, "分析并直接列出单词'" + word + "'的词根词缀组成结构。" +
        "要求它们恰好能不多不少拼成这个单词，只要分析单词本身，不要分析其派生词");
      System.out.println(resp);
      q = resp + "\n\n" + q;
      JSON.Valuer valuer = JSON.newValuer(parse(assistant.chat(apiKey, model, q)));
      WordDict.Struct struct = new WordDict.Struct();
      List<WordDict.Part> parts = new ArrayList<>();
      valuer.get("parts").asArray().forEach(p -> {
        WordDict.Part part = WordDict.Part.builder()
          .part(p.get("part").asText())
          .root(p.get("isRoot").asBoolean())
          .prefix(p.get("isPrefix").asBoolean())
          .infix(p.get("isInfix").asBoolean())
          .suffix(p.get("isSuffix").asBoolean())
          .meaning(p.get("meaning_en").asText())
          .meaningTrans(p.get("meaning_zh").asText())
          .build();
        parts.add(part);
      });
      struct.setParts(parts);
      struct.setAnalysis(valuer.get("analysis_en").asText());
      struct.setAnalysisTrans(valuer.get("analysis_zh").asText());
      dict.setStruct(struct);
    }, "struct");
  }
}