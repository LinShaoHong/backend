package com.github.sun.picture;

import com.github.sun.foundation.sql.NamingStrategy;
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
@Table(name = "picture")
public class Picture {
  @Id
  private String id;
  private String title;
  private String source;
  private String type;
  private String category;
  private String tags;
  private String localPath;
  private String originUrl;
  private long likes;
  private long visits;
  @Transient
  private Date createTime;
  @Transient
  private Date updateTime;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @NamingStrategy
  @Table(name = "picture_details")
  public static class Detail {
    @Id
    private String id;
    private String picId;
    private String source;
    private String localPath;
    private String originUrl;
    @Transient
    private Date createTime;
    @Transient
    private Date updateTime;
  }
}
