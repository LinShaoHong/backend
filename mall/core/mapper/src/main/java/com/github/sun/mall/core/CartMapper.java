package com.github.sun.mall.core;

import com.github.sun.foundation.mybatis.CompositeMapper;
import com.github.sun.mall.core.entity.Cart;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Set;

@Mapper
public interface CartMapper extends CompositeMapper<Cart> {
  @Select("SELECT * FROM mall_cart WHERE userId = #{userId}")
  List<Cart> findByUserId(@Param("userId") String userId);

  @Select("SELECT * FROM mall_cart WHERE userId = #{userId} AND checked = TRUE")
  List<Cart> findByUserIdAndChecked(@Param("userId") String userId);

  @Select("SELECT * FROM mall_cart WHERE userId = #{userId} AND goodsId = #{goodsId} AND productId = #{productId}")
  Cart findByUserIdAndGoodsIdAndProductId(@Param("userId") String userId,
                                          @Param("goodsId") String goodsId,
                                          @Param("productId") String productId);

  @Select("DELETE FROM mall_cart WHERE userId = #{userId} AND productId IN(${productId})")
  void deleteByUserIdAndProductIdIn(@Param("userId") String userId,
                                    @Param("productIds") Set<String> productId);
}
