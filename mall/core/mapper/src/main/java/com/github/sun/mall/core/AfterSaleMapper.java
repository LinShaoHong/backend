package com.github.sun.mall.core;

import com.github.sun.foundation.mybatis.CompositeMapper;
import com.github.sun.mall.core.entity.AfterSale;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AfterSaleMapper extends CompositeMapper<AfterSale> {
  @Select("SELECT COUNT(0) FROM mall_after_sale WHERE userId = #{userId} AND userId = #{userId} AND afterSaleSn = #{afterSaleSn}")
  int countByUserIdAndAfterSaleSn(@Param("userId") String userId, @Param("afterSaleSn") String afterSaleSn);

  @Select("SELECT COUNT(0) FROM mall_after_sale WHERE userId = #{userId} AND userId = #{userId} AND orderId = #{orderId}")
  AfterSale findByUserIdAndOrderId(@Param("userId") String userId, @Param("orderId") String orderId);

  @Delete("DELETE FROM mall_after_sale WHERE userId = #{userId} AND userId = #{userId} AND orderId = #{orderId}")
  void deleteByUserIdAndOrderId(@Param("userId") String userId, @Param("orderId") String orderId);
}
