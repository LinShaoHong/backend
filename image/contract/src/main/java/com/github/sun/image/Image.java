package com.github.sun.image;

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
@Table(name = "image")
public class Image {
  @Id
  private String id;
  private String title;
  private String source;
  private String type;
  private String category;
  private String categorySpell;
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
  @Table(name = "image_details")
  public static class Detail {
    @Id
    private String id;
    private String imgId;
    private String source;
    private String localPath;
    private String originUrl;
    @Transient
    @JsonIgnore
    private Date createTime;
    @Transient
    @JsonIgnore
    private Date updateTime;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @NamingStrategy
  @Table(name = "image_category")
  public static class Category {
    @Id
    private String id;
    private String type;
    private String label;
    private String name;
    private long count;
    private String parentId;
    @Transient
    @JsonIgnore
    private Date createTime;
    @Transient
    @JsonIgnore
    private Date updateTime;
  }
}
