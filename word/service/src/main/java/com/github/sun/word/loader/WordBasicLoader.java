package com.github.sun.word.loader;

import com.github.sun.foundation.ai.Assistant;
import com.github.sun.foundation.boot.Injector;
import com.github.sun.foundation.boot.utility.Dates;
import com.github.sun.foundation.boot.utility.Reflections;
import com.github.sun.foundation.boot.utility.Throws;
import com.github.sun.foundation.boot.utility.Tuple;
import com.github.sun.foundation.sql.IdGenerator;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.word.WordDict;
import com.github.sun.word.WordDictMapper;
import com.ibm.icu.impl.data.ResourceReader;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;
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
        List<String> dictFS = Arrays.stream(WordDict.class.getDeclaredFields())
                .map(Field::getName).collect(Collectors.toList());
        mapper.noPass(word);
        List<String> fs = Arrays.asList(fields);
        fs.forEach(f -> mapper.loading(word, "'$." + f + "Loading'"));
        for (int i = 0; i < 2; i++) {
            try {
                run.accept(dict);
                ex = null;
                List<String> _fs = fs.stream().filter(dictFS::contains).collect(Collectors.toList());
                if (!_fs.isEmpty()) {
                    SqlBuilder sb = factory.create();
                    SqlBuilder.Template template = sb.from(WordDict.class)
                            .where(sb.field("id").eq(word)).update()
                            .set(_fs, f -> Tuple.of(f, Reflections.getValue(dict, f)))
                            .template();
                    mapper.updateByTemplate(template);
                }
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
        fs.forEach(f -> mapper.loaded(word, "'$." + f + "Loading'"));
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
            db.setUsAudioId(IdGenerator.next());
            db.setUkAudioId(IdGenerator.next());
            WordDict.LoadState loadState = new WordDict.LoadState();
            db.setLoadState(loadState);
            db.setLoadTime(new Date());
            int sort = genWordSort(userId);
            db.setSort(sort);
            mapper.insert(db);
        }
        return db;
    }

    public static synchronized int genWordSort(int userId) {
        WordLoaderCodeMapper mapper = Injector.getInstance(WordLoaderCodeMapper.class);
        WordLoaderCheckMapper checkMapper = Injector.getInstance(WordLoaderCheckMapper.class);
        String date = Dates.format(new Date());
        long code = 1;
        String id = date + ":" + code;
        WordLoaderCode entity = mapper.queryForUpdate(id);
        if (entity != null) {
            code = entity.getCode() + 1;
            mapper.updateById(entity.getId(), code);
        } else {
            WordLoaderCode v = new WordLoaderCode();
            v.setId(id);
            v.setType(date);
            v.setCode(code);
            mapper.insert(v);

            WordLoaderCheck check = new WordLoaderCheck();
            check.setId(date + ":" + userId);
            check.setUserId(userId);
            check.setDate(date);
            check.setSort(1);
            checkMapper.insert(check);
        }
        return ((Long) code).intValue();
    }
}