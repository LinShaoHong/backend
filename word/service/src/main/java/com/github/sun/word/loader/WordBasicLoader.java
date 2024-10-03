package com.github.sun.word.loader;

import com.github.sun.foundation.ai.Assistant;
import com.github.sun.foundation.boot.Injector;
import com.github.sun.foundation.boot.utility.*;
import com.github.sun.foundation.sql.IdGenerator;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.word.WordDict;
import com.github.sun.word.WordDictMapper;
import com.ibm.icu.impl.data.ResourceReader;
import org.springframework.util.StringUtils;

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
    @Resource
    protected WordLoaderConfig config;
    @Resource
    protected WordDictMapper mapper;
    @Resource
    protected WordLoaderErrorMapper errorMapper;
    @Resource(name = "mysql")
    protected SqlBuilder.Factory factory;

    protected void retry(String word, JSON.Valuer attr, int userId, Consumer<WordDict> run, String... fields) {
        String part = attr.get("$part").asText();
        Throwable ex = null;
        WordDict dict = init(word, userId);
        List<String> dictFS = Arrays.stream(WordDict.class.getDeclaredFields())
                .map(Field::getName).collect(Collectors.toList());
        mapper.noPass(word);
        List<String> fs = Arrays.asList(fields);
        mapper.loading(word, "'$." + part + "Loading'");
        mapper.fromModel(word, "'$." + part + "'", parseAiName(attr));

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
        mapper.loaded(word, "'$." + part + "Loading'");
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
            db.setLoadState(new WordDict.LoadState());
            db.setFromModel(new WordDict.FromModel());
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

    protected String callAi(JSON.Valuer attr, String userMessage) {
        WordLoaderConfig.Ai.Loader loader = parseConfigAiLoader(attr);

        String name = attr.get("$name").asText("");
        name = StringUtils.hasText(name) ? name : loader == null || !StringUtils.hasText(loader.getName()) ?
                config.getAi().getUse() : loader.getName();

        String model = attr.get("$model").asText("");
        if (!StringUtils.hasText(model)) {
            if (loader != null &&
                    StringUtils.hasText(loader.getName()) &&
                    StringUtils.hasText(loader.getModel()) && loader.getName().equals(name)) {
                model = loader.getModel();
            } else {
                model = name.equals("qwen") ?
                        config.getAi().getQwen().getModel() : config.getAi().getDoubao().getModel();
            }
        }
        Assistant assistant = (Assistant) Injector.getInstance(name);

        String apiKey = name.equals("qwen") ?
                config.getAi().getQwen().getKey() : config.getAi().getDoubao().getKey();
        return assistant.apiKey(apiKey)
                .model(model)
                .systemMessage("你是一名中英文双语教育专家，拥有帮助将中文视为母语的用户理解和记忆英语单词的专长。")
                .userMessage(userMessage)
                .fetch();
    }

    public static WordLoaderConfig.Ai.Loader parseConfigAiLoader(JSON.Valuer attr) {
        String part = attr.get("$part").asText();
        WordLoaderConfig config = Injector.getInstance(WordLoaderConfig.class);
        return config.getAi().getLoaders().stream()
                .filter(v -> Objects.equals(v.getPart(), part)).findFirst().orElse(null);
    }

    public static String parseAiName(JSON.Valuer attr) {
        WordLoaderConfig.Ai.Loader loader = parseConfigAiLoader(attr);
        String name = attr.get("$name").asText("");
        WordLoaderConfig config = Injector.getInstance(WordLoaderConfig.class);
        name = StringUtils.hasText(name) ? name : loader == null || !StringUtils.hasText(loader.getName()) ?
                config.getAi().getUse() : loader.getName();
        return name;
    }
}