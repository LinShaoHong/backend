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
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "mall_topic")
public class Topic implements Entity<String> {
  @Id
  private String id;
  @NotEmpty(message = "缺少标题")
  private String title;
  private String subtitle;
  @NotNull(message = "缺少价格")
  private BigDecimal price;
  private String readCount;
  private String picUrl;
  private int sortOrder;
  @Converter(JsonHandler.ListStringHandler.class)
  private List<String> goodsIds;
  @NotEmpty(message = "缺少内容")
  private String content;
  @Transient
  @JsonIgnore
  private Date createTime;
  @Transient
  @JsonIgnore
  private Date updateTime;
}
