package com.github.sun.word.loader;

import com.github.sun.foundation.ai.Assistant;
import com.github.sun.foundation.boot.Injector;
import com.github.sun.foundation.boot.utility.Reflections;
import com.github.sun.foundation.boot.utility.Throws;
import com.github.sun.foundation.boot.utility.Tuple;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.word.*;
import com.ibm.icu.impl.data.ResourceReader;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class WordBasicLoader implements WordLoader {
    private final static Map<String, String> MDS = new HashMap<>();
    private final static ClassLoader loader = ResourceReader.class.getClassLoader();
    @Value("${qwen.key}")
    protected String apiKey;
    @Value("${qwen.model}")
    protected String model;
    @Resource
    protected WordDictMapper mapper;
    @Resource
    protected WordLoaderErrorMapper errorMapper;
    @Resource(name = "qwen")
    protected Assistant assistant;
    @Resource(name = "mysql")
    protected SqlBuilder.Factory factory;

    protected void retry(String word, int userId, Consumer<WordDict> run, String... fields) {
        Throwable ex = null;
        WordDict dict = init(word, userId);
        mapper.noPass(word);
        Arrays.asList(fields).forEach(f -> mapper.loading(word, "'$." + f + "Loading'"));
        for (int i = 0; i < 2; i++) {
            try {
                run.accept(dict);
                ex = null;
                SqlBuilder sb = factory.create();
                SqlBuilder.Template template = sb.from(WordDict.class)
                        .where(sb.field("id").eq(word)).update()
                        .set(Arrays.asList(fields), f -> Tuple.of(f, Reflections.getValue(dict, f)))
                        .template();
                mapper.updateByTemplate(template);
                break;
            } catch (Throwable e) {
                ex = e;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException x) {
                    // do nothing
                }
            }
        }
        if (ex != null) {
            WordLoaderError error = errorMapper.findById(word);
            if (error == null) {
                error = new WordLoaderError();
                error.setId(word);
                error.setError(Throws.stackTraceOf(ex));
                errorMapper.insert(error);
            } else {
                error.setError(Throws.stackTraceOf(ex));
                errorMapper.update(error);
            }
        }
        Arrays.asList(fields).forEach(f -> mapper.loaded(word, "'$." + f + "Loading'"));
    }

    protected String parse(String resp) {
        int i = resp.indexOf("```json");
        if (i < 0) {
            return resp;
        }
        int j = resp.lastIndexOf("```");
        return resp.substring(i + 7, j);
    }

    protected String loadQ(String file) {
        return MDS.computeIfAbsent(file, k -> {
            try (InputStream in = loader.getResourceAsStream(file)) {
                assert in != null;
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                return reader.lines().collect(Collectors.joining("\n"));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    public static synchronized WordDict init(String word, int userId) {
        WordDictMapper mapper = Injector.getInstance(WordDictMapper.class);
        WordDict db = mapper.findById(word);
        if (db == null) {
            db = new WordDict();
            db.setId(word);
            WordDict.LoadState loadState = new WordDict.LoadState();
            db.setLoadState(loadState);
            db.setLoadTime(new Date());
            WordCodeService codeService = Injector.getInstance(WordCodeService.class);
            int sort = codeService.genWordSort(userId);
            db.setSort(sort);
            mapper.insert(db);
        }
        return db;
    }
}