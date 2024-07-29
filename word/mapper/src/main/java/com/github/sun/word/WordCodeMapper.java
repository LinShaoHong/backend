package com.github.sun.word;

import com.github.sun.foundation.mybatis.CompositeMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface WordCodeMapper extends CompositeMapper<WordCode> {
  @Select("select * from word_code where id=#{id} for update")
  WordCode queryForUpdate(@Param("id") String id);

  @Update("update word_code set `code`=#{code} where id=#{id}")
  int updateById(@Param("id") String id, @Param("code") long code);
}