package com.github.sun.word;

import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@RefreshScope
public class WordDictService {
    @Resource
    private WordDictMapper mapper;

    public WordDict byId(String id) {
        return mapper.findById(id);
    }
}