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
@Table(name = "mall_user")
public class User {
  @Id
  private String id;
  private String username;
  private String password;
  private int gender;
  private String birthday;
  private long lastLoginTime;
  private String lastLoginIp;
  private int userLevel;
  private String nickname;
  private String mobile;
  private String avatar;
  private String wxOpenId;
  private String sessionKey;
  private Byte status;
  private boolean deleted;
  @Transient
  private Date createTime;
  @Transient
  private Date updateTime;
}
