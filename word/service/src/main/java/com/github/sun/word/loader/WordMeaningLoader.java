package com.github.sun.word.loader;

import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.word.WordDict;
import com.github.sun.word.spider.WordJsSpider;
import com.github.sun.word.spider.WordYdSpider;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

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
            Set<String> set = WordYdSpider.fetchMeaning(dict);
            set.addAll(WordJsSpider.fetchMeaning(dict));
            String ms = set.stream().map(v -> {
                if ("nouns".equals(v)) {
                    return "名词";
                } else if ("verbs".equals(v)) {
                    return "动词";
                } else if ("adjectives".equals(v)) {
                    return "形容词";
                } else if ("adverbs".equals(v)) {
                    return "副词";
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
                } else if ("auxiliary".equals(v)) {
                    return "助词";
                } else if ("modal".equals(v)) {
                    return "情态动词";
                }
                return null;
            }).filter(Objects::nonNull).collect(Collectors.joining("、"));
            String q = loadQ("cues/释义.md");
            q = q.replace("$word", word).replace("$scope", ms);
            JSON.Valuer valuer = JSON.newValuer(parse(assistant.chat(apiKey, model, q)));
            dict.setMeaning(WordDict.TranslatedMeaning.builder()
                    .nouns(prettify(valuer.get("translated_meanings").get("nouns").asText("")))
                    .verbs(prettify(valuer.get("translated_meanings").get("verbs").asText("")))
                    .adjectives(prettify(valuer.get("translated_meanings").get("adjectives").asText("")))
                    .adverbs(prettify(valuer.get("translated_meanings").get("adverbs").asText("")))
                    .preposition(prettify(valuer.get("translated_meanings").get("preposition").asText("")))
                    .pronoun(prettify(valuer.get("translated_meanings").get("pronoun").asText("")))
                    .conjunction(prettify(valuer.get("translated_meanings").get("conjunction").asText("")))
                    .article(prettify(valuer.get("translated_meanings").get("article").asText("")))
                    .interjection(prettify(valuer.get("translated_meanings").get("interjection").asText("")))
                    .numeral(prettify(valuer.get("translated_meanings").get("numeral").asText("")))
                    .determiner(prettify(valuer.get("translated_meanings").get("determiner").asText("")))
                    .auxiliary(prettify(valuer.get("translated_meanings").get("auxiliary").asText("")))
                    .modal(prettify(valuer.get("translated_meanings").get("modalVerb").asText("")))
                    .build());
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