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
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "mall_cart")
public class Cart {
  @Id
  private String id;
  private String userId;
  @NotNull(message = "缺少商品")
  private String goodsId;
  private String goodsSn;
  private String goodsName;
  @NotNull(message = "缺少产品")
  private String productId;
  private String price;
  @Min(value = 1, message = "数量最少是1个")
  private int number;
  private List<String> specifications;
  private boolean checked;
  private String picUrl;
  @Transient
  @JsonIgnore
  private Date createTime;
  @Transient
  @JsonIgnore
  private Date updateTime;
}
