package com.github.sun.pn;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.sun.foundation.modelling.NamingStrategy;
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
@Table(name = "pn_item")
public class Item {
  @Id
  private String id;
  @NotEmpty(message = "缺少名字")
  private String name;
  private String type;
  private String category;
  private String categorySpell;
  private long visits;
  private String lastVisitIP;
  @Transient
  private Date createTime;
  @Transient
  private Date updateTime;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @NamingStrategy
  @Table(name = "pn_girl_category")
  public static class Category {
    @Id
    private String id;
    private String type;
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
