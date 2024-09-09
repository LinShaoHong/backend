package com.github.sun.word.loader;

import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.foundation.sql.IdGenerator;
import com.github.sun.word.WordDict;
import com.github.sun.word.spider.WordJsSpider;
import com.github.sun.word.spider.WordYdSpider;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@RefreshScope
@Service("collocation")
public class WordCollocationLoader extends WordBasicLoader {
    @Override
    public void load(String word, JSON.Valuer attr, int userId) {
        retry(word, userId, dict -> {
            List<WordDict.Phrase> phrases = new ArrayList<>();
            List<WordDict.Formula> formulas = new ArrayList<>();
            Set<String> set = WordYdSpider.fetchMeaning(dict);
            set.addAll(WordJsSpider.fetchMeaning(dict));
            boolean verbs = set.contains("verbs");
            String q = verbs ? loadQ("cues/公式词组.md") : loadQ("cues/短语词组.md");
            try {
                String resp = assistant.chat(apiKey, model, q.replace("$word", word));
                JSON.Valuer valuer = JSON.newValuer(parse(resp));
                if (verbs) {
                    valuer.get("formulas").asArray().forEach(a -> {
                        WordDict.Formula formula = new WordDict.Formula();
                        formula.setEn(a.get("formula").asText());
                        formula.setZh(a.get("formula_explain_zh").asText());
                        List<WordDict.ExampleSentence> examples = new ArrayList<>();
                        a.get("examples").asArray().forEach(e -> {
                            WordDict.ExampleSentence example = new WordDict.ExampleSentence();
                            example.setAudioId(IdGenerator.next());

                            String sentence = e.get("sentence").asText();
                            sentence = sentence.replaceAll("’", "'");
                            sentence = sentence.replaceAll("，", ",");
                            sentence = sentence.replaceAll("。", ".");
                            example.setSentence(sentence);

                            example.setTranslation(e.get("translation").asText());
                            examples.add(example);
                        });
                        formula.setExamples(examples);
                        formulas.add(formula);
                    });
                }

                valuer.get("phrases").asArray().forEach(a -> {
                    WordDict.Phrase phrase = new WordDict.Phrase();

                    String p = a.get("phrase").asText();
                    p = p.replaceAll("’", "'");
                    p = p.replaceAll("，", ",");
                    p = p.replaceAll("。", ".");
                    phrase.setEn(p);

                    phrase.setZh(a.get("translation").asText());
                    if (formulas.stream().noneMatch(f -> Objects.equals(f.getEn(), phrase.getEn()))) {
                        phrases.add(phrase);
                    }
                });
            } catch (Throwable ex) {
                //do nothing
            }
            dict.setCollocation(new WordDict.Collocation(formulas, phrases));
        }, "collocation");
    }
}