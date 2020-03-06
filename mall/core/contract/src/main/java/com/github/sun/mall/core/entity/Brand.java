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
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "mall_brand")
public class Brand implements Entity<String> {
  @Id
  private String id;
  @NotEmpty(message = "缺少品牌名称")
  private String name;
  @NotEmpty(message = "缺少品牌描述")
  private String desc;
  private String picUrl;
  private int sortOrder;
  @NotNull(message = "缺少浮动价格")
  private BigDecimal floorPrice;
  @Transient
  @JsonIgnore
  private Date createTime;
  @Transient
  @JsonIgnore
  private Date updateTime;
}
