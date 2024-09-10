package com.github.sun.word;

import com.github.sun.foundation.mybatis.CompositeMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface WordDictLemmaMapper extends CompositeMapper<WordDictLemma> {
    @Select("select * from word_dict_lemma where json_contains(inflections,'\"${word}\"')")
    WordDictLemma byInf(@Param("word") String word);

    @Update("update word_dict_lemma set has=1 where id=#{id}")
    void has(@Param("id") String id);

    @Update("update word_dict_lemma set has=0 where id=#{id}")
    void noHas(@Param("id") String id);
}