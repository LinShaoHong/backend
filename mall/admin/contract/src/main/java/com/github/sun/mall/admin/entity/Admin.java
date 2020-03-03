package com.github.sun.mall.admin.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.sun.foundation.boot.exception.UnexpectedException;
import com.github.sun.foundation.boot.utility.Hex;
import com.github.sun.foundation.modelling.Converter;
import com.github.sun.foundation.modelling.JsonHandler;
import com.github.sun.foundation.modelling.NamingStrategy;
import com.github.sun.mall.core.entity.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "mall_admin_admin")
public class Admin implements Entity<String> {
  @Id
  private String id;
  @NotEmpty(message = "名称不合法")
  @Pattern(message = "名称不合法", regexp = "^[\\w\\u4e00-\\u9fa5]{6,20}(?<!_)$")
  private String username;
  private String password;
  private String lastLoginIp;
  private Date lastLoginTime;
  private String avatar;
  @Converter(JsonHandler.SetStringHandler.class)
  private Set<String> roleIds;
  @Transient
  @JsonIgnore
  private Date createTime;
  @Transient
  @JsonIgnore
  private Date updateTime;

  public static String hashPassword(String password) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] input = password.getBytes(StandardCharsets.UTF_8);
      byte[] output = md.digest(input);
      return Hex.bytes2readable(output);
    } catch (NoSuchAlgorithmException ex) {
      throw new UnexpectedException("呃！出错了");
    }
  }
}
