package com.github.sun.pn;

import com.github.sun.foundation.boot.exception.UnexpectedException;
import com.github.sun.foundation.boot.utility.Hex;
import com.github.sun.foundation.modelling.NamingStrategy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "pn_user")
public class User {
  @Id
  private String id;
  private String username;
  private String nickName;
  private String password;
  private String avatar;
  private String email;
  private boolean vip;
  private Date vipStartTime;
  private Date vipEndTime;
  private Date lastLoginTime;
  private String lastLoginIp;
  @Transient
  private Date createTime;
  @Transient
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
