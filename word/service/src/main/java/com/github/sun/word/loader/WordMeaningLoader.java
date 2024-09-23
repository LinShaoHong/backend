package com.github.sun.word.loader;

import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.foundation.boot.utility.Reflections;
import com.github.sun.word.WordDict;
import com.github.sun.word.spider.WordJsSpider;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RefreshScope
@Service("meaning")
public class WordMeaningLoader extends WordBasicLoader {
    @Override
    public void load(String word, JSON.Valuer attr, int userId) {
        retry(word, userId, dict -> {
            Set<String> set = WordJsSpider.fetchMeaning(dict);
            String ms = set.stream().map(v -> {
                if ("noun".equals(v)) {
                    return "名词";
                } else if ("verb".equals(v)) {
                    return "动词";
                } else if ("transitiveVerb".equals(v)) {
                    return "及物动词";
                } else if ("intransitiveVerb".equals(v)) {
                    return "不及物动词";
                } else if ("auxiliaryVerb".equals(v)) {
                    return "助动词";
                } else if ("modalVerb".equals(v)) {
                    return "情态动词";
                } else if ("adverb".equals(v)) {
                    return "副词";
                } else if ("adjective".equals(v)) {
                    return "形容词";
                } else if ("preposition".equals(v)) {
                    return "介词";
                } else if ("pronoun".equals(v)) {
                    return "代词";
                } else if ("conjunction".equals(v)) {
                    return "连词";
                } else if ("article".equals(v)) {
                    return "冠词";
                } else if ("interjection".equals(v)) {
                    return "感叹词";
                } else if ("numeral".equals(v)) {
                    return "数词";
                } else if ("determiner".equals(v)) {
                    return "限定词";
                }
                return null;
            }).filter(Objects::nonNull).collect(Collectors.joining("、"));
            String q = loadQ("cues/释义.md");
            q = q.replace("$word", word).replace("$scope", ms);
            JSON.Valuer valuer = JSON.newValuer(parse(assistant.chat(apiKey, model, q)));

            WordDict.TranslatedMeaning meaning = new WordDict.TranslatedMeaning();
            for (Field f : WordDict.TranslatedMeaning.class.getDeclaredFields()) {
                String v = !set.contains(f.getName()) ? null :
                        prettify(valuer.get("translated_meanings").get(f.getName()).asText(""));
                Reflections.setValue(meaning, f, v);
            }
            dict.setMeaning(meaning);
        }, "meaning");
    }

    private String prettify(String m) {
        if (m.trim().isEmpty()) {
            return "";
        }
        String[] arr = m.split(" ");
        return Arrays.stream(arr).map(v -> {
            String[] ar = v.split(";");
            return Arrays.stream(ar)
                    .map(d -> {
                        String s = d.trim().replaceAll(",", "，");
                        if (!s.endsWith("；") && !s.endsWith("，") && !s.endsWith("、")) {
                            return s + "；";
                        }
                        return s;
                    })
                    .collect(Collectors.joining(""));
        }).collect(Collectors.joining(""));
    }
}