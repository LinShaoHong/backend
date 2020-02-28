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
@Table(name = "mall_notice")
public class Notice {
  @Id
  private String id;
  private String title;
  private String content;
  private String adminId;
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
  @Table(name = "mall_notice_admin")
  public static class Admin {
    @Id
    private String id;
    private String noticeId;
    private String noticeTitle;
    private String adminId;
    private long readTime;
    private boolean deleted;
    @Transient
    private Date createTime;
    @Transient
    private Date updateTime;
  }
}
