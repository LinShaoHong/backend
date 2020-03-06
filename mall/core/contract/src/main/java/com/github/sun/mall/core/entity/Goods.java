package com.github.sun.mall.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "mall_goods")
public class Goods implements Entity<String> {
  @Id
  private String id;
  private String sn;
  private String name;
  private String categoryId;
  private String brandId;
  @Converter(JsonHandler.ListStringHandler.class)
  private List<String> gallery;
  private String keywords;
  private String brief;
  @JsonProperty("isOnSale")
  private boolean isOnSale;
  private int sortOrder;
  private String picUrl;
  private String shareUrl;
  @JsonProperty("isNew")
  private boolean isNew;
  @JsonProperty("isHot")
  private boolean isHot;
  private String unit;
  private BigDecimal counterPrice;
  private BigDecimal retailPrice;
  private String detail;
  @Converter(AttributeHandler.class)
  private List<Attribute> attributes;
  @Converter(ProductHandler.class)
  private List<Product> products;
  @Converter(SpecificationHandler.class)
  private List<Specification> specifications;
  @Transient
  @JsonIgnore
  private Date createTime;
  @Transient
  @JsonIgnore
  private Date updateTime;

  public Product findByProductId(String productId) {
    return products == null ? null :
      products.stream().filter(v -> Objects.equals(productId, v.getProductId()))
        .findFirst()
        .orElse(null);
  }

  public void recomputeRetailPrice() {
    this.retailPrice = this.products == null ? null :
      this.products.stream()
        .min(Comparator.comparing(Product::getPrice))
        .orElseGet(() -> Product.builder().build())
        .getPrice();
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Attribute {
    private String attribute;
    private String value;
  }

  public static class AttributeHandler extends JsonHandler.ListHandler<Attribute> {
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Product {
    private String productId;
    private List<String> specifications;
    private BigDecimal price;
    private int number;
    private String url;
  }

  public static class ProductHandler extends JsonHandler.ListHandler<Product> {
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Specification {
    private String specification;
    private String value;
    private String picUrl;
  }

  public static class SpecificationHandler extends JsonHandler.ListHandler<Specification> {
  }
}
