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
@Table(name = "mall_groupon")
public class Groupon implements Entity<String> {
  public enum Status {
    NONE, ON, SUCCEED, FAIL
  }

  @Id
  private String id;
  private String orderId;
  private String grouponId;
  private String rulesId;
  private String userId;
  private String shareUrl;
  private String creatorId;
  private long creatorTime;
  private Status status;
  @Transient
  @JsonIgnore
  private Date createTime;
  @Transient
  @JsonIgnore
  private Date updateTime;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @NamingStrategy
  @Table(name = "mall_groupon_rules")
  public static class Rules implements Entity<String> {
    public enum Status {
      ON, DOWN_EXPIRE, DOWN_ADMIN
    }

    @Id
    private String id;
    private String goodsId;
    private String picUrl;
    private String discount;
    private int discountMember;
    private long expireTime;
    private Status status;
    @Transient
    @JsonIgnore
    private Date createTime;
    @Transient
    @JsonIgnore
    private Date updateTime;
  }
}
