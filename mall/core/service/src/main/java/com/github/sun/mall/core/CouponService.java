package com.github.sun.mall.core;

import com.github.sun.foundation.mybatis.BasicService;
import com.github.sun.mall.core.entity.Coupon;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

@Service
public class CouponService extends BasicService<String, Coupon, CouponMapper> {
  @Resource
  private CouponMapper.User couponUserMapper;

  public Coupon check(String userId, String couponId, String userCouponId, BigDecimal checkedGoodsPrice) {
    Coupon coupon = mapper.findById(couponId);
    if (coupon == null) {
      return null;
    }
    Coupon.User couponUser = couponUserMapper.findById(userCouponId);
    if (couponUser == null) {
      couponUser = couponUserMapper.findByUserIdAndCouponId(userId, couponId);
    } else if (!couponId.equals(couponUser.getCouponId())) {
      return null;
    }
    if (couponUser == null) {
      return null;
    }
    // 检查是否超期
    Coupon.TimeType timeType = coupon.getTimeType();
    int days = coupon.getDays();
    Date now = new Date();
    if (timeType == Coupon.TimeType.TIME) {
      if (now.before(new Date(coupon.getStartTime())) || now.after(new Date(coupon.getEndTime()))) {
        return null;
      }
    } else if (timeType.equals(Coupon.TimeType.DAYS)) {
      Calendar c = Calendar.getInstance();
      c.setTime(couponUser.getCreateTime());
      c.add(Calendar.DAY_OF_MONTH, days);
      if (now.after(c.getTime())) {
        return null;
      }
    } else {
      return null;
    }

    // 检测商品是否符合
    // TODO 目前仅支持全平台商品，所以不需要检测
    if (coupon.getGoodsType() != Coupon.GoodsType.ALL) {
      return null;
    }

    // 检测订单状态
    if (coupon.getStatus() != Coupon.Status.NORMAL) {
      return null;
    }
    // 检测是否满足最低消费
    if (checkedGoodsPrice.compareTo(new BigDecimal(coupon.getMin())) < 0) {
      return null;
    }
    return coupon;
  }
}
