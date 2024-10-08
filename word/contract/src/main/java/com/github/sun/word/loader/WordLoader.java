package com.github.sun.word.loader;

import com.github.sun.foundation.boot.utility.JSON;

public interface WordLoader {
    void load(String word, JSON.Valuer attr, int userId);

    default void load(String word, String part, int userId) {
        load(word, JSON.newValuer("{\"$part\":\"" + part + "\"}"), userId);
    }
}