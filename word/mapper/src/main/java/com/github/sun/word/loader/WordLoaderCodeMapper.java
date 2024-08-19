package com.github.sun.word.loader;

import com.github.sun.foundation.mybatis.CompositeMapper;
import com.github.sun.word.loader.WordLoaderCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface WordLoaderCodeMapper extends CompositeMapper<WordLoaderCode> {
  @Select("select * from word_loader_code where id=#{id} for update")
  WordLoaderCode queryForUpdate(@Param("id") String id);

  @Update("update word_loader_code set `code`=#{code} where id=#{id}")
  int updateById(@Param("id") String id, @Param("code") long code);
}