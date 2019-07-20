package com.github.sun.picture;

import com.github.sun.foundation.sql.NamingStrategy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "picture")
public class Picture {
  @Id
  private String id;
  private String title;
  private String type;
  private String tags;
  private String localUrl;
  private String originUrl;
  private long likes;
  private long visits;
  private long createTime;
  private long updateTime;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @NamingStrategy
  @Table(name = "picture_details")
  public static class Details {
    @Id
    private String id;
    private String picId;
    private String localUrl;
    private String originUrl;
    private long createTime;
    private long updateTime;
  }
}
