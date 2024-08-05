package com.github.sun.word.loader;

import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.word.WordAffix;
import com.github.sun.word.WordAffixMapper;
import com.github.sun.word.WordDict;
import com.github.sun.word.spider.WordJsSpider;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@RefreshScope
@Service("struct")
public class WordStructLoader extends WordBasicLoader {
  @Resource
  private WordAffixMapper affixMapper;

  @Override
  public void load(String word, JSON.Valuer attr, int userId) {
    WordAffix affix = affixMapper.findById(word);
    retry(word, userId, dict -> {
      String q = loadQ("cues/词根词缀.md");
      String resp;
      String root = attr == null ? null : attr.get("root").asText();
      root = StringUtils.hasText(root) ? root : (affix != null ? affix.getRoot() : null);
      root = StringUtils.hasText(root) ? root : WordJsSpider.fetchRoot(dict);
      if (StringUtils.hasText(root)) {
        resp = assistant.chat(apiKey, model, q.replace("$input", "请简要分析单词" + word + "的词根为" + root + "时的词根词缀组成结构，不要给出其他情况"));
      } else {
        resp = assistant.chat(apiKey, model, q.replace("$input", "分析并直接列出单词'" + word + "'的词根词缀组成结构。"));
      }
      JSON.Valuer valuer = JSON.newValuer(parse(resp));
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