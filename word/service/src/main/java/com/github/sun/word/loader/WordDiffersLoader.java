package com.github.sun.word.loader;

import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.foundation.sql.IdGenerator;
import com.github.sun.word.WordDict;
import com.github.sun.word.WordDictDiff;
import com.github.sun.word.WordDictDiffMapper;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@RefreshScope
@Service("differs")
public class WordDiffersLoader extends WordBasicLoader {
    @Resource
    private WordDictDiffMapper mapper;
    @Resource
    private WordLoaderDiffMapper loaderDiffMapper;

    @Override
    public void load(String word, JSON.Valuer attr, int userId) {
        retry(word, userId, dict -> {
            loaderDiffMapper.byWord(word).forEach(d -> {
                if (mapper.byDiffId(d.getId()).isEmpty()) {
                    String q = loadQ("cues/辨析.md");
                    String ws = String.join("、", d.getWords());
                    try {
                        String resp = callAi(q.replace("$word", ws));
                        JSON.Valuer valuer = JSON.newValuer(parse(resp));
                        String meaning = valuer.get("common_meaning").asText("");
                        valuer.get("words").asArray().forEach(a -> {
                            WordDictDiff differ = new WordDictDiff();
                            differ.setId(IdGenerator.next());
                            differ.setMean(meaning);
                            differ.setDiffId(d.getId());
                            differ.setWords(d.getWords());
                            differ.setWord(a.get("word").asText());

                            differ.setDefinition(a.get("emphasized_aspect_zh").asText(""));
                            differ.setScenario(a.get("usage_scenario_zh").asText(""));
                            List<WordDict.ExampleSentence> examples = new ArrayList<>();
                            a.get("examples").asArray().forEach(e -> {
                                WordDict.ExampleSentence example = new WordDict.ExampleSentence();
                                example.setAudioId(IdGenerator.next());

                                String sentence = e.get("sentence").asText("");
                                sentence = sentence.replaceAll("’", "'");
                                sentence = sentence.replaceAll("，", ",");
                                sentence = sentence.replaceAll("。", ".");
                                example.setSentence(sentence);

                                example.setTranslation(e.get("translation").asText(""));
                                examples.add(example);
                            });
                            differ.setExamples(examples);
                            mapper.replace(differ);
                        });
                    } catch (Throwable ex) {
                        // do nothing
                    }
                }
            });
        }, "differs");
    }
}