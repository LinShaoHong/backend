package com.github.sun.spider.entity;

import com.github.sun.foundation.sql.NamingStrategy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "picture")
public class Picture {
  private String id;
  private String title;
  private String type;
  private String tags;
  private String localUrl;
  private String originUrl;
  private long likedNum;
  private long viewedNum;
  private long createTime;
  private long updateTime;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @NamingStrategy
  @Table(name = "picture_detail")
  public static class Detail {
    private String id;
    private String picId;
    private String localUrl;
    private String originUrl;
    private long createTime;
    private long updateTime;
  }
}
