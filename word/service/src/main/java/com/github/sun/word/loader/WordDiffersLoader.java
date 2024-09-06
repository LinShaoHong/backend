package com.github.sun.word.loader;

import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.foundation.sql.IdGenerator;
import com.github.sun.word.WordDict;
import com.github.sun.word.WordDictDiff;
import com.github.sun.word.WordDictDiffMapper;
import com.github.sun.word.spider.WordJsSpider;
import com.github.sun.word.spider.WordXxEnSpider;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@RefreshScope
@Service("differs")
public class WordDiffersLoader extends WordBasicLoader {
    @Resource
    private WordDictDiffMapper mapper;

    @Override
    public void load(String word, JSON.Valuer attr, int userId) {
        retry(word, userId, dict -> {
            Set<String> ws = new LinkedHashSet<>();
            ws.add(dict.getId());
            ws.addAll(WordJsSpider.fetchDiffs(dict));
            ws.addAll(WordXxEnSpider.fetchDiffs(dict));
            if (ws.size() > 1) {
                Set<String> exists = mapper.findByIds(ws).stream().map(WordDictDiff::getId).collect(Collectors.toSet());
                List<String> _ws = ws.stream().filter(v -> !exists.contains(v)).collect(Collectors.toList());
                if (!_ws.isEmpty()) {
                    String q = loadQ("cues/辨析.md");
                    String w = String.join("、", _ws);
                    try {
                        String resp = assistant.chat(apiKey, model,
                                q.replace("$word", w));
                        JSON.Valuer valuer = JSON.newValuer(parse(resp));
                        valuer.asArray().forEach(a -> {
                            WordDictDiff differ = new WordDictDiff();
                            differ.setId(a.get("word").asText());
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
                        dict.setDiffers(new ArrayList<>(ws));
                    } catch (Throwable ex) {
                        // do nothing
                        ArrayList<String> es = new ArrayList<>(exists);
                        ArrayList<String> __ws = new ArrayList<>(ws);
                        es.sort(Comparator.comparingInt(__ws::indexOf));
                        dict.setDiffers(es);
                    }
                }
            }
        }, "differs");
    }
}