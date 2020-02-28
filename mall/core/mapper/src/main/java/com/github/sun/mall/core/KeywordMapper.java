package com.github.sun.mall.core;

import com.github.sun.foundation.mybatis.CompositeMapper;
import com.github.sun.mall.core.entity.Keyword;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KeywordMapper extends CompositeMapper<Keyword> {
  @Select("SELECT * FROM mall_keyword WHERE isDefault = TRUE")
  List<Keyword> findDefault();

  @Select("SELECT * FROM mall_keyword WHERE isHot = TRUE")
  List<Keyword> findHot();

  @Select("SELECT * FROM mall_keyword WHERE keyword LIKE CONCAT('%',#{q}, '%') LIMIT #{start}, #{count}")
  List<Keyword> findHint(@Param("q") String q, @Param("q") int start, @Param("q") int count);
}
