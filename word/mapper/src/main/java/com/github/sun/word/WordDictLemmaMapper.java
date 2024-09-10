package com.github.sun.word;

import com.github.sun.foundation.mybatis.CompositeMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WordDictLemmaMapper extends CompositeMapper<WordDictLemma> {
    @Select("select * from word_dict_lemma where has=1 and json_contains(inflections,'\"${word}\"')")
    WordDictLemma byInf(@Param("word") String word);
}