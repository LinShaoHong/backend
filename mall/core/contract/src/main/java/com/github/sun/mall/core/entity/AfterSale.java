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
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "mall_after_sale")
public class AfterSale {
  public enum Type {
    GOODS_MISS,
    GOODS_NEEDLESS,
    GOODS_REQUIRED
  }

  public enum Status {
    REQUEST,
    ACCEPT,
    REFUND,
    REJECT,
    CANCEL
  }

  @Id
  private String id;
  private String afterSaleSn;
  @NotNull(message = "缺少对应的订单")
  private String orderId;
  private String userId;
  private Type type;
  @NotNull(message = "缺少原因")
  private String reason;
  @NotNull(message = "缺少退款金额")
  private String amount;
  private List<String> pictures;
  private String comment;
  private Status status;
  private long handleTime;
  @Transient
  @JsonIgnore
  private Date createTime;
  @Transient
  @JsonIgnore
  private Date updateTime;
}
