package com.github.sun.mall.core;

import com.github.sun.foundation.mybatis.CompositeMapper;
import com.github.sun.foundation.mybatis.interceptor.anno.Flatten;
import com.github.sun.mall.core.entity.AfterSale;
import com.github.sun.mall.core.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Set;

@Mapper
public interface OrderMapper extends CompositeMapper<Order> {
  @Update("UPDATE mall_order SET afterSaleStatus = #{afterSaleStatus} WHERE id = #{id}")
  void updateAfterSaleStatus(@Param("id") String id, @Param("afterSaleStatus") AfterSale.Status afterSaleStatus);

  @Select("SELECT * FROM mall_order WHERE userId = #{userId}")
  List<Order> findByUserId(@Param("userId") String userId);

  @Mapper
  interface Goods extends CompositeMapper<Order.Goods> {
    @Select("SELECT * FROM mall_order_goods WHERE userId = #{userId} AND orderId = #{orderId}")
    List<Order.Goods> findByUserIdAndOrderId(@Param("userId") String userId, @Flatten @Param("orderId") String orderId);

    @Select("SELECT * FROM mall_order_goods WHERE userId = #{userId} AND orderId IN(${orderIds})")
    List<Order.Goods> findByUserIdAndOrderIdIn(@Param("userId") String userId, @Flatten @Param("orderIds") Set<String> orderIds);
  }
}
