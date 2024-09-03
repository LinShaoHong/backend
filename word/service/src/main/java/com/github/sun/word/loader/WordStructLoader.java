package com.github.sun.word.loader;

import com.github.sun.foundation.boot.utility.JSON;
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
    private WordLoaderAffixMapper affixMapper;

    @Override
    public void load(String word, JSON.Valuer attr, int userId) {
        WordLoaderAffix affix = affixMapper.findById(word);
        retry(word, userId, dict -> {
            String q = loadQ("cues/词根词缀.md");
            q = q.replace("$word", word);
            String resp;
            String root = attr == null ? null : attr.get("root").asText();
            root = StringUtils.hasText(root) ? root : (affix != null ? affix.getRoot() : null);
            root = StringUtils.hasText(root) ? root : WordJsSpider.fetchRoot(dict);
            if (StringUtils.hasText(root)) {
                if (root.contains("-") || root.contains("+")) {
                    resp = assistant.chat(apiKey, model, q.replace("$input", "限定" + word + "的结构为" + root + "，以此分析并直接给出其(词根、前缀、中缀、后缀)结构"));
                } else if (!Objects.equals(root, word)) {
                    resp = assistant.chat(apiKey, model, q.replace("$input", "限定" + word + "的词根为" + root + "，分析并直接给出其(词根、前缀、中缀、后缀)结构"));
                } else {
                    resp = assistant.chat(apiKey, model, q.replace("$input", word + "是一个基本的单词，并无词缀"));
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
            struct.setParts(parts);
            struct.setAnalysis(valuer.get("memory_formula_zh").asText());
            struct.setHistory(valuer.get("origin_history_zh").asText());
            if (StringUtils.hasText(struct.getAnalysis())) {
                struct.setAnalysis(struct.getAnalysis().replaceAll("-", ""));
            }
            dict.setStruct(struct);
        }, "struct");
    }
}