package com.github.sun.mall.core;

import com.github.sun.foundation.mybatis.CompositeMapper;
import com.github.sun.mall.core.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CouponMapper extends CompositeMapper<Coupon> {
  @Mapper
  interface User extends CompositeMapper<Coupon.User> {
    @Select("SELECT * FROM mall_coupon_user WHERE userId = #{userId}")
    List<Coupon.User> findByUserId(@Param("userId") String userId);

    @Select("SELECT * FROM mall_coupon_user WHERE userId = #{userId} AND couponId = #{couponId}")
    Coupon.User findByUserIdAndCouponId(@Param("userId") String userId, @Param("couponId") String couponId);
  }
}
