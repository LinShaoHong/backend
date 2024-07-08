package com.github.sun.card;

import com.github.sun.foundation.mybatis.CompositeMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CardSmsSpecMapper extends CompositeMapper<CardSmsSpec> {
  @Select("select distinct content from card_sms_spec where type=#{type} order by sort asc")
  List<String> byType(@Param("type") String type);
}