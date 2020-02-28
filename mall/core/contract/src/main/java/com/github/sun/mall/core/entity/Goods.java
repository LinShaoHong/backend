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
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "mall_goods")
public class Goods {
  @Id
  private String id;
  private String sn;
  private String name;
  private Integer categoryId;
  private Integer brandId;
  private List<String> gallery;
  private String keywords;
  private String brief;
  private boolean isOnSale;
  private Short sortOrder;
  private String picUrl;
  private String shareUrl;
  private boolean isNew;
  private boolean isHot;
  private String unit;
  private String counterPrice;
  private String retailPrice;
  private String detail;
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
  @Table(name = "mall_goods_attribute")
  public static class Attribute {
    @Id
    private String id;
    private String goodsId;
    private String attribute;
    private String value;
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
  @Table(name = "mall_goods_product")
  public static class Product {
    @Id
    private String id;
    private String goodsId;
    private List<String> specifications;
    private String price;
    private int number;
    private String url;
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
  @Table(name = "mall_goods_specification")
  public static class Specification {
    @Id
    private String id;
    private String goodsId;
    private String specification;
    private String value;
    private String picUrl;
    @Transient
    @JsonIgnore
    private Date createTime;
    @Transient
    @JsonIgnore
    private Date updateTime;
  }
}
