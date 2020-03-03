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
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "mall_category")
public class Category implements Entity<String> {
  @Id
  private String id;
  @NotEmpty(message = "缺少类目名称")
  private String name;
  private String keywords;
  private String desc;
  private String pId;
  private String iconUrl;
  private String picUrl;
  @Min(value = 1, message = "层级最少为1")
  @Max(value = 1, message = "层级最大为2")
  private int level;
  private int sortOrder;
  @Transient
  @JsonIgnore
  private Date createTime;
  @Transient
  @JsonIgnore
  private Date updateTime;
}
