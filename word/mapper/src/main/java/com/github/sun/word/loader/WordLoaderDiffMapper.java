package com.github.sun.word.loader;

import com.github.sun.foundation.mybatis.CompositeMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WordLoaderDiffMapper extends CompositeMapper<WordLoaderDiff> {
    @Select("select * from word_loader_diff where JSON_CONTAINS(words,'\"${word}\"')")
    List<WordLoaderDiff> byWord(@Param("word") String word);
}