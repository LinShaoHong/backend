package com.github.sun.word.loader;

import com.github.sun.foundation.mybatis.CompositeMapper;
import com.github.sun.word.loader.WordLoaderCheck;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface WordLoaderCheckMapper extends CompositeMapper<WordLoaderCheck> {
  @Update("update word_loader_check set curr=0 where userId=#{userId} and id<>#{id}")
  void past(@Param("id")String id, @Param("userId")int userId );

  @Select("select distinct date from word_loader_check order by date")
  List<String> dates();
}