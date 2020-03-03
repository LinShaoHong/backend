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
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "mall_advertise")
public class Advertise implements Entity<String> {
  @Id
  private String id;
  @NotEmpty(message = "缺少推广名称")
  private String name;
  private String link;
  private String url;
  private int position;
  @NotEmpty(message = "缺少推广内容")
  private String content;
  private Date startTime;
  private Date endTime;
  private boolean enabled;
  @Transient
  @JsonIgnore
  private Date createTime;
  @Transient
  @JsonIgnore
  private Date updateTime;
}
