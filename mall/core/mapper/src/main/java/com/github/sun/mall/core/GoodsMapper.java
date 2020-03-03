package com.github.sun.mall.core;

import com.github.sun.foundation.mybatis.CompositeMapper;
import com.github.sun.mall.core.entity.Goods;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GoodsMapper extends CompositeMapper<Goods> {
  @Mapper
  interface Attribute extends CompositeMapper<Goods.Attribute> {
    @Delete("DELETE FROM mall_goods_attribute WHERE goodsId = #{goodsId}")
    void deleteByGoodsId(@Param("goodsId") String goodsId);
  }

  @Mapper
  interface Product extends CompositeMapper<Goods.Product> {
    @Delete("DELETE FROM mall_goods_product WHERE goodsId = #{goodsId}")
    void deleteByGoodsId(@Param("goodsId") String goodsId);
  }

  @Mapper
  interface Specification extends CompositeMapper<Goods.Specification> {
    @Delete("DELETE FROM mall_goods_specification WHERE goodsId = #{goodsId}")
    void deleteByGoodsId(@Param("goodsId") String goodsId);
  }
}
