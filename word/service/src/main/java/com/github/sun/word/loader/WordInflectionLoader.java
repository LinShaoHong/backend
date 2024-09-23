package com.github.sun.word.loader;

import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.word.WordDict;
import com.github.sun.word.spider.WordJsSpider;
import com.github.sun.word.spider.WordYdSpider;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@RefreshScope
@Service("inflection")
public class WordInflectionLoader extends WordBasicLoader {
    @Override
    public void load(String word, JSON.Valuer attr, int userId) {
        retry(word, userId, dict -> {
            try {
                WordYdSpider.fetchPhonetic(dict);
                if (!StringUtils.hasText(dict.getUsPhonetic()) ||
                        !StringUtils.hasText(dict.getUkPhonetic())) {
                    WordJsSpider.fetchPhonetic(dict);
                }
                dict.setInflection(new WordDict.Inflection());
                WordYdSpider.fetchInflection(dict);
                WordJsSpider.fetchInflection(dict);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }, "ukPhonetic", "usPhonetic", "inflection");
    }
}