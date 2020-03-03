package com.github.sun.mall.admin.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "mall_admin_notice")
public class Notice implements Entity<String> {
  @Id
  private String id;
  @NotEmpty(message = "缺少标题")
  private String title;
  private String content;
  private String adminId;
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
  @Table(name = "mall_admin_notice_admin")
  public static class Admin implements Entity<String> {
    @Id
    private String id;
    private String noticeId;
    private String noticeTitle;
    private String adminId;
    private Date readTime;
    @Transient
    @JsonIgnore
    private Date createTime;
    @Transient
    @JsonIgnore
    private Date updateTime;
  }
}
