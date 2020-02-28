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
@Table(name = "mall_groupon")
public class Groupon {
  @Id
  private String id;
  private String orderId;
  private String grouponId;
  private String rulesId;
  private String userId;
  private String shareUrl;
  private String creatorUserId;
  private long creatorUserTime;
  private int status;
  private boolean deleted;
  @Transient
  private Date createTime;
  @Transient
  private Date updateTime;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @NamingStrategy
  @Table(name = "mall_groupon_rules")
  public static class Rules {
    @Id
    private String id;
    private String goodsId;
    private String picUrl;
    private String discount;
    private int discountMember;
    private long expireTime;
    private int status;
    private boolean deleted;
    @Transient
    private Date createTime;
    @Transient
    private Date updateTime;
  }
}
