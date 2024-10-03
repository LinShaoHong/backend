package com.github.sun.word.loader;

import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.foundation.boot.utility.Reflections;
import com.github.sun.word.WordDict;
import com.github.sun.word.spider.WordJsSpider;
import com.github.sun.word.spider.WordYdSpider;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@RefreshScope
@Service("inflection")
public class WordInflectionLoader extends WordBasicLoader {
    @Override
    public void load(String word, JSON.Valuer attr, int userId) {
        retry(word, attr, userId, dict -> {
            try {
                WordYdSpider.fetchPhonetic(dict);
                if (!StringUtils.hasText(dict.getUsPhonetic()) ||
                        !StringUtils.hasText(dict.getUkPhonetic())) {
                    WordJsSpider.fetchPhonetic(dict);
                }
                dict.setInflection(new WordDict.Inflection());
                List<String> set = dict.getMeaning() != null ? dict.getMeaning().getSorts() : new ArrayList<>();
                set.removeIf(sp -> {
                    String means = (String) Reflections.getValue(dict.getMeaning(), sp);
                    return !StringUtils.hasText(means);
                });
                WordYdSpider.fetchInflection(dict, set);
                WordJsSpider.fetchInflection(dict, set);

            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }, "ukPhonetic", "usPhonetic", "inflection");
    }
}