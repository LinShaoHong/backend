package com.github.sun.mall.core.entity;

import com.github.sun.foundation.modelling.NamingStrategy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "mall_category")
public class Category {
  @Id
  private String id;
  private String name;
  private String keywords;
  private String desc;
  private String pid;
  private String iconUrl;
  private String picUrl;
  private int level;
  private int sortOrder;
  private boolean deleted;
  @Transient
  private Date createTime;
  @Transient
  private Date updateTime;
}
