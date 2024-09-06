package com.github.sun.word.loader;

import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.foundation.sql.IdGenerator;
import com.github.sun.word.WordDict;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@RefreshScope
@Service("examples")
public class WordExamplesLoader extends WordBasicLoader {
    @Override
    public void load(String word, JSON.Valuer attr, int userId) {
        retry(word, userId, dict -> {
            String q = loadQ("cues/例句.md");
            q = q.replace("$word", word);
            JSON.Valuer valuer = JSON.newValuer(parse(assistant.chat(apiKey, model, q)));
            List<WordDict.ExampleSentence> examples = new ArrayList<>();
            valuer.get("example_sentences").asArray().forEach(e -> {
                String sentence = e.get("sentence").asText();
                sentence = sentence.replaceAll("’", "'");
                sentence = sentence.replaceAll("，", ",");
                sentence = sentence.replaceAll("。", ".");

                String translation = e.get("translation").asText();

                examples.add(new WordDict.ExampleSentence(IdGenerator.next(), sentence, translation));
            });
            dict.setExamples(examples);
        }, "examples");
    }
}