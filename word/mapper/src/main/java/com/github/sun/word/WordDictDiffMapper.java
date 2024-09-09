package com.github.sun.word;

import com.github.sun.foundation.mybatis.CompositeMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WordDictDiffMapper extends CompositeMapper<WordDictDiff> {
    @Select("select * from word_dict_diff where word=#{word}")
    List<WordDictDiff> byWord(@Param("word") String word);

    @Select("select * from word_dict_diff where diffId=#{diffId}")
    List<WordDictDiff> byDiffId(@Param("diffId") String diffId);
}