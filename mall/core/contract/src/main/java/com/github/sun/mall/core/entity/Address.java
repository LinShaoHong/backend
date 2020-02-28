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
import javax.validation.constraints.Pattern;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "mall_address")
public class Address implements Entity<String> {
  @Id
  private String id;
  private String name;
  private String userId;
  @NotNull(message = "缺少所在省市")
  private String province;
  @NotNull(message = "缺少所在城市")
  private String city;
  @NotNull(message = "缺少所在国家")
  private String county;
  private String detail;
  @NotNull(message = "缺少邮政编码")
  private String areaCode;
  private String postalCode;
  @NotNull(message = "缺少手机号")
  @Pattern(message = "手机号格式不正确", regexp = "^((13[0-9])|(14[5,7])|(15[0-3,5-9])|(16[6])|(17[0,1,3,5-8])|(18[0-9])|(19[8,9]))\\d{8}$")
  private String tel;
  private boolean isDefault;
  @Transient
  @JsonIgnore
  private Date createTime;
  @Transient
  @JsonIgnore
  private Date updateTime;
}
