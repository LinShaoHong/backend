package com.github.sun.word;

import com.github.sun.foundation.mybatis.CompositeMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WordAffixMapper extends CompositeMapper<WordAffix> {
  @Select("select distinct id from word_affix where root=#{root}")
  List<String> byRoot(@Param("root") String root);
}