package com.github.sun.word.loader;

import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.foundation.boot.utility.Reflections;
import com.github.sun.foundation.sql.IdGenerator;
import com.github.sun.word.WordDict;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RefreshScope
@Service("examples")
public class WordExamplesLoader extends WordBasicLoader {
    @Override
    public void load(String word, JSON.Valuer attr, int userId) {
        retry(word, attr, userId, dict -> {
            String q = loadQ("cues/例句.md");
            List<String> set = dict.getMeaning() != null ? dict.getMeaning().getSorts() : new ArrayList<>();
            set.remove("abbreviation");
            set.removeIf(sp -> {
                String means = (String) Reflections.getValue(dict.getMeaning(), sp);
                return !StringUtils.hasText(means);
            });
            if (!set.isEmpty()) {
                int[] arr = distribute(set.size());
                int i = 0;
                List<WordDict.Example> examples = new ArrayList<>();
                for (String sp : set) {
                    String _q = q.replace("#word", word);
                    _q = _q.replace("#num", "" + arr[i++]);
                    _q = _q.replace("#partOfSpeech", WordMeaningLoader.SPEECHES.get(sp));
                    String means = (String) Reflections.getValue(dict.getMeaning(), sp);
                    if (StringUtils.hasText(means)) {
                        means = Arrays.stream(means.split("，"))
                                .flatMap(s -> Arrays.stream(s.split("；")))
                                .map(s -> "\t- " + s).collect(Collectors.joining("\n"));
                        _q = _q.replace("$means", "noun".equals(sp) ? "" : "- 要求该单词在例句中的涵义如下：\n" + means);

                        JSON.Valuer valuer = JSON.newValuer(parse(callAi(attr, _q)));
                        WordDict.Example example = new WordDict.Example();
                        example.setSpeech(sp);
                        List<WordDict.ExampleSentence> sentences = new ArrayList<>();
                        valuer.get("example_sentences").asArray().forEach(e -> {
                            String sentence = e.get("sentence").asText();
                            sentence = sentence.replaceAll("’", "'");
                            sentence = sentence.replaceAll("，", ",");
                            sentence = sentence.replaceAll("。", ".");
                            String translation = e.get("translation").asText();
                            sentences.add(new WordDict.ExampleSentence(IdGenerator.next(), sentence, translation));
                        });
                        example.setSentences(sentences);
                        examples.add(example);
                    }
                }
                dict.setExamples(examples);
            }
        }, "examples");
    }

    private int[] distribute(int parts) {
        int total = 10;
        int[] result = new int[parts];
        int base = total / parts;
        int remainder = total % parts;
        Arrays.fill(result, base);
        for (int i = 0; i < remainder; i++) {
            result[i]++;
        }
        return result;
    }
}