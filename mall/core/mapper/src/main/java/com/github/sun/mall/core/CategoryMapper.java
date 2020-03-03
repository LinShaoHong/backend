package com.github.sun.mall.core;

import com.github.sun.foundation.mybatis.CompositeMapper;
import com.github.sun.mall.core.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CategoryMapper extends CompositeMapper<Category> {
  @Select("SELECT * FROM mall_category WHERE level = #{level}")
  List<Category> findByLevel(@Param("level") int level);

  @Select("SELECT * FROM mall_category WHERE pId = #{pId}")
  List<Category> findByPId(@Param("pId") String pId);
}
