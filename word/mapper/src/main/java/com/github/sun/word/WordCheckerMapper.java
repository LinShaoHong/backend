package com.github.sun.word;

import com.github.sun.foundation.mybatis.CompositeMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WordCheckerMapper extends CompositeMapper<WordChecker> {
  @Select("select * from word_checker order by id")
  List<WordChecker> all();
}