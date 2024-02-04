package com.github.sun.card;

import com.github.sun.foundation.mybatis.CompositeMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CardCodeMapper extends CompositeMapper<CardCode> {
  @Select("select * from card_code where id=#{id} for update")
  CardCode queryForUpdate(@Param("id") String id);

  @Update("update card_code set `code`=#{code} where id=#{id}")
  int updateById(@Param("id") String id, @Param("code") Long code);
}