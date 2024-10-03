package com.github.sun.word.loader;

import com.github.sun.foundation.boot.utility.JSON;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

@RefreshScope
@Service("origin")
public class WordOriginLoader extends WordBasicLoader {
    @Override
    public void load(String word, JSON.Valuer attr, int userId) {
        retry(word, attr, userId, dict -> {
            //词源历史
            String q = loadQ("cues/词源历史.md");
            String resp = callAi(attr, q.replace("$word", word));
            JSON.Valuer valuer = JSON.newValuer(parse(resp));
            dict.setOrigin(valuer.get("origin_history_zh").asText());
        }, "origin");
    }
}