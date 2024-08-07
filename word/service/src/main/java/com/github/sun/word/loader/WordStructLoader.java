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
import java.util.Objects;

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
        if (root.contains("-") || root.contains("+")) {
          resp = assistant.chat(apiKey, model, q.replace("$input", word + "的结构为" + root + "，以此分析并直接给出其(词根、前缀、中缀、后缀)结构"));
        } else {
          resp = assistant.chat(apiKey, model, q.replace("$input", word + "的词根为" + root + "，分析并直接给出其(词根、前缀、中缀、后缀)结构"));
        }
      } else {
        resp = assistant.chat(apiKey, model, q.replace("$input", "分析并直接给出单词'" + word + "'的(词根、前缀、中缀、后缀)结构"));
      }
      JSON.Valuer valuer = JSON.newValuer(parse(resp));
      WordDict.Struct struct = new WordDict.Struct();
      List<WordDict.Part> parts = new ArrayList<>();
      String _root = root;
      valuer.get("parts").asArray().forEach(p -> {
        WordDict.Part part = WordDict.Part.builder()
          .part(p.get("part").asText())
          .root(p.get("isRoot").asBoolean(false))
          .prefix(p.get("isPrefix").asBoolean(false))
          .infix(p.get("isInfix").asBoolean(false))
          .suffix(p.get("isSuffix").asBoolean(false))
          .meaning(p.get("meaning_en").asText(""))
          .meaningTrans(p.get("meaning_zh").asText(""))
          .build();
        if (Objects.equals(_root, part.getPart())) {
          part.setRoot(true);
        }
        if (part.isRoot() || part.isPrefix() || part.isInfix() || part.isSuffix()) {
          String w = part.getPart();
          w = w.replaceAll("-", "");
          part.setPart(w);
          parts.add(part);
        }
      });
//      String w = "";
//      Iterator<WordDict.Part> it = parts.iterator();
//      while (it.hasNext()) {
//        String _w = w + it.next().getPart();
//        if (dict.getId().startsWith(_w)) {
//          w = _w;
//        } else {
//          it.remove();
//        }
//      }
      struct.setParts(parts);
      struct.setAnalysis(valuer.get("struct_analysis_zh").asText());
      struct.setHistory(valuer.get("historical_cultural_zh").asText());
      dict.setStruct(struct);
    }, "struct");
  }
}