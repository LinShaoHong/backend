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
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "mall_order")
public class Order {
  public enum Status {
    CREATE,
    PAY,
    SHIP,
    CONFIRM,
    CANCEL,
    AUTO_CANCEL,
    ADMIN_CANCEL,
    REFUND,
    REFUND_CONFIRM,
    AUTO_CONFIRM
  }

  @Id
  private String id;
  private String userId;
  private String orderSn;
  private Status status;
  private AfterSale.Status afterSaleStatus;
  private String consignee;
  private String mobile;
  private String address;
  private String message;
  private String goodsPrice;
  private String freightPrice;
  private String couponPrice;
  private String integralPrice;
  private String grouponPrice;
  private String orderPrice;
  private String actualPrice;
  private String payId;
  private long payTime;
  private String shipSn;
  private String shipChannel;
  private LocalDateTime shipTime;
  private String refundAmount;
  private String refundType;
  private String refundContent;
  private long refundTime;
  private long confirmTime;
  private int comments;
  private long endTime;
  @Transient
  @JsonIgnore
  private Date createTime;
  @Transient
  @JsonIgnore
  private Date updateTime;

  public boolean isConfirm() {
    return this.status == Status.CONFIRM;
  }

  public boolean isAutoConfirm() {
    return this.status == Status.AUTO_CONFIRM;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @NamingStrategy
  @Table(name = "mall_order_goods")
  public static class Goods {
    @Id
    private String id;
    private String userId;
    private String orderId;
    private String goodsId;
    private String goodsName;
    private String goodsSn;
    private String productId;
    private Short number;
    private String price;
    private List<String> specifications;
    private String picUrl;
    private int comment;
    @Transient
    @JsonIgnore
    private Date createTime;
    @Transient
    @JsonIgnore
    private Date updateTime;
  }
}
