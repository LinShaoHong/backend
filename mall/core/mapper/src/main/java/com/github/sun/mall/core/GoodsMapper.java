package com.github.sun.mall.core;

import com.github.sun.foundation.mybatis.CompositeMapper;
import com.github.sun.mall.core.entity.Goods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GoodsMapper extends CompositeMapper<Goods> {
  @Select("SELECT SUM(IF(products IS NULL, 0, JSON_LENGTH(products))) FROM mall_goods")
  int countProduct();

  @Select("SELECT * FROM mall_goods WHERE name = #{name}")
  List<Goods> findByName(@Param("name") String name);
}
