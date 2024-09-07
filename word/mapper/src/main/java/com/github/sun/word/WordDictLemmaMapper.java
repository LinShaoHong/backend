package com.github.sun.word;

import com.github.sun.foundation.mybatis.CompositeMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WordDictLemmaMapper extends CompositeMapper<WordDictLemma> {
}