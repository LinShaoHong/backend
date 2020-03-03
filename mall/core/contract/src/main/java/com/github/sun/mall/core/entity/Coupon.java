package com.github.sun.mall.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.sun.foundation.modelling.Converter;
import com.github.sun.foundation.modelling.JsonHandler;
import com.github.sun.foundation.modelling.NamingStrategy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "mall_coupon")
public class Coupon implements Entity<String> {
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
  @NotEmpty(message = "缺少优惠券名称")
  private String name;
  private String desc;
  private String tag;
  private int total;
  private BigDecimal discount;
  private BigDecimal min;
  private int limit;
  private Type type;
  private Status status;
  private GoodsType goodsType;
  @Converter(JsonHandler.ListStringHandler.class)
  private List<String> goodsValue;
  private String code;
  private TimeType timeType;
  private int days;
  private Date startTime;
  private Date endTime;
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
    private Date usedTime;
    private Date startTime;
    private Date endTime;
    private String orderId;
    @Transient
    @JsonIgnore
    private Date createTime;
    @Transient
    @JsonIgnore
    private Date updateTime;
  }
}
