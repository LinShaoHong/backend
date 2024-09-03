package com.github.sun.word.loader;

import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.word.WordDict;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RefreshScope
@Service("collocation")
public class WordCollocationLoader extends WordBasicLoader {
    @Override
    public void load(String word, JSON.Valuer attr, int userId) {
        retry(word, userId, dict -> {
            List<WordDict.Phrase> phrases = new ArrayList<>();
            List<WordDict.Formula> formulas = new ArrayList<>();
            String q = loadQ("cues/短语词组.md");
            try {
                String resp = assistant.chat(apiKey, model, q.replace("$word", word));
                JSON.Valuer valuer = JSON.newValuer(parse(resp));
                valuer.get("formulas").asArray().forEach(a -> {
                    WordDict.Formula formula = new WordDict.Formula();
                    formula.setEn(a.get("formula").asText());
                    formula.setZh(a.get("formula_explain_zh").asText());
                    List<WordDict.ExampleSentence> examples = new ArrayList<>();
                    a.get("examples").asArray().forEach(e -> {
                        WordDict.ExampleSentence example = new WordDict.ExampleSentence();
                        example.setSentence(e.get("sentence").asText());
                        example.setTranslation(e.get("translation").asText());
                        examples.add(example);
                    });
                    formula.setExamples(examples);
                    formulas.add(formula);
                });
                valuer.get("phrases").asArray().forEach(a -> {
                    WordDict.Phrase phrase = new WordDict.Phrase();
                    phrase.setEn(a.get("phrase").asText());
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