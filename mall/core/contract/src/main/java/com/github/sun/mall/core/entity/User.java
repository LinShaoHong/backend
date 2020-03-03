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
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "mall_user")
public class User implements Entity<String> {
  @Id
  private String id;
  private String username;
  private String password;
  private int gender;
  private String birthday;
  private Date lastLoginTime;
  private String lastLoginIp;
  private int userLevel;
  private String nickname;
  private String mobile;
  private String avatar;
  private String wxOpenId;
  private String sessionKey;
  private int status;
  @Transient
  @JsonIgnore
  private Date createTime;
  @Transient
  @JsonIgnore
  private Date updateTime;
}
