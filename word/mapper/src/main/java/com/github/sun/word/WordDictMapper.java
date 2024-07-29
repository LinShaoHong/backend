package com.github.sun.word;

import com.github.sun.foundation.mybatis.CompositeMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface WordDictMapper extends CompositeMapper<WordDict> {
  @Update("update word_dict set passed = 1, passTime=now() where id=#{id}")
  void pass(@Param("id") String id);

  @Update("update word_dict set viewed = 1 where id=#{id}")
  void viewed(@Param("id") String id);

  @Update("update word_dict set passed = 0 where id=#{id}")
  void noPass(@Param("id") String id);

  @Update("update word_dict set loadState = JSON_SET(loadState, ${field}, true) where id=#{id}")
  void loading(@Param("id") String id, @Param("field") String field);

  @Update("update word_dict set loadState = JSON_SET(loadState, ${field}, false) where id=#{id}")
  void loaded(@Param("id") String id, @Param("field") String field);

  @Select("select count(0) from word_dict where date_format(loadTime,'%Y-%m-%d')=#{date}")
  int countByDate(@Param("date") String date);

  @Select("select count(0) from word_dict where date_format(loadTime,'%Y-%m-%d')=#{date} and passed=1")
  int countByPassed(@Param("date") String date);

  @Select("select * from word_dict where date_format(loadTime,'%Y-%m-%d')=#{date} and sort=#{sort}")
  WordDict byDateAndSort(@Param("date") String date, @Param("sort") int sort);

  @Select("select * from word_dict where date_format(loadTime,'%Y-%m-%d')=#{date} order by sort")
  List<WordDict> byDate(@Param("date") String date);
}