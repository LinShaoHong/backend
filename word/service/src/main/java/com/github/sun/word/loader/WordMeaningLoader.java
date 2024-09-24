package com.github.sun.word.loader;

import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.foundation.boot.utility.Reflections;
import com.github.sun.word.WordDict;
import com.github.sun.word.spider.WordJsSpider;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

@RefreshScope
@Service("meaning")
public class WordMeaningLoader extends WordBasicLoader {
    public static final Map<String, String> SPEECHES = new LinkedHashMap<>() {{
        put("noun", "名词");
        put("verb", "动词");
        put("transitiveVerb", "及物动词");
        put("intransitiveVerb", "不及物动词");
        put("auxiliaryVerb", "助动词");
        put("modalVerb", "情态动词");
        put("adverb", "副词");
        put("adjective", "形容词");
        put("preposition", "介词");
        put("pronoun", "代词");
        put("conjunction", "连词");
        put("article", "冠词");
        put("interjection", "感叹词");
        put("numeral", "数词");
        put("determiner", "限定词");
    }};

    @Override
    public void load(String word, JSON.Valuer attr, int userId) {
        retry(word, userId, dict -> {
            List<String> set = WordJsSpider.fetchMeaning(dict);
            String ms = set.stream().map(SPEECHES::get)
                    .filter(Objects::nonNull)
                    .map(s -> "\t-" + s)
                    .collect(Collectors.joining("\n"));
            String q = loadQ("cues/释义.md");
            q = q.replace("$word", word).replace("$scope", ms);
            JSON.Valuer valuer = JSON.newValuer(parse(assistant.chat(apiKey, model, q)));

            WordDict.TranslatedMeaning meaning = new WordDict.TranslatedMeaning();
            for (Field f : WordDict.TranslatedMeaning.class.getDeclaredFields()) {
                String v = !set.contains(f.getName()) ? null :
                        prettify(valuer.get("translated_meanings").get(f.getName()).asText(""));
                Reflections.setValue(meaning, f, v);
            }
            meaning.setSorts(new ArrayList<>(set));
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