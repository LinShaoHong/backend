package com.github.sun.qm;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.sun.foundation.modelling.Converter;
import com.github.sun.foundation.modelling.JsonHandler;
import com.github.sun.foundation.modelling.NamingStrategy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "qm_girl")
public class Girl {
  public enum Type {
    SN, QM, PIC, VIDEO
  }

  @Id
  private String id;
  @NotEmpty(message = "缺少名字")
  private String name;
  @NotEmpty(message = "缺少描述")
  private String title;
  private String contact;
  private BigDecimal price;
  private Type type;
  private String category;
  private String categorySpell;
  private String mainImage;
  private String city;
  @Converter(JsonHandler.ListStringHandler.class)
  private List<String> detailImages;
  @Converter(JsonHandler.ListStringHandler.class)
  private List<String> contactImages;
  private String video;
  private long likes;
  private long visits;
  private long collects;
  private long comments;
  private long payments;
  private boolean onService;
  @Transient
  private Date createTime;
  @Transient
  private Date updateTime;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @NamingStrategy
  @Table(name = "qm_girl_category")
  public static class Category {
    @Id
    private String id;
    private Type type;
    private String name;
    private String nameSpell;
    private long count;
    @Transient
    @JsonIgnore
    private Date createTime;
    @Transient
    @JsonIgnore
    private Date updateTime;
  }
}
