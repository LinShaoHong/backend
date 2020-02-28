package com.github.sun.mall.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.sun.foundation.modelling.NamingStrategy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "mall_coupon")
public class Coupon implements Entity<String> {
  public static final Short TYPE_COMMON = 0;
  public static final Short TYPE_REGISTER = 1;
  public static final Short TYPE_CODE = 2;

  public static final Short GOODS_TYPE_ALL = 0;
  public static final Short GOODS_TYPE_CATEGORY = 1;
  public static final Short GOODS_TYPE_ARRAY = 2;

  public static final Short STATUS_NORMAL = 0;
  public static final Short STATUS_EXPIRED = 1;
  public static final Short STATUS_OUT = 2;

  public static final Short TIME_TYPE_DAYS = 0;
  public static final Short TIME_TYPE_TIME = 1;

  public enum Type {
    COMMON, REGISTER, CODE
  }

  public enum GoodsType {
    ALL, CATEGORY, ARRAY
  }

  public enum Status {
    NORMAL, EXPIRED, OUT
  }

  public enum TimeType {
    DAYS, TIME
  }

  @Id
  private String id;
  private String name;
  private String desc;
  private String tag;
  private int total;
  private String discount;
  private String min;
  private int limit;
  private Type type;
  private Status status;
  private GoodsType goodsType;
  private List<String> goodsValue;
  private String code;
  private TimeType timeType;
  private int days;
  private long startTime;
  private long endTime;
  @Transient
  @JsonIgnore
  private Date createTime;
  @Transient
  @JsonIgnore
  private Date updateTime;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @NamingStrategy
  @Table(name = "mall_coupon_user")
  public static class User implements Entity<String> {
    private String id;
    private String userId;
    private String couponId;
    private int status;
    private long usedTime;
    private long startTime;
    private long endTime;
    private String orderId;
    @Transient
    @JsonIgnore
    private Date createTime;
    @Transient
    @JsonIgnore
    private Date updateTime;
  }
}
