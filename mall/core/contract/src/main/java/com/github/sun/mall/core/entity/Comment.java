package com.github.sun.mall.core.entity;

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
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "mall_comment")
public class Comment implements Entity<String> {
  public enum Type {
    GOODS, TOPIC, ORDER
  }

  @Id
  private String id;
  @NotNull(message = "缺少评价商品")
  private String valueId;
  private Type type;
  @NotNull(message = "缺少内容")
  private String content;
  private String adminContent;
  private String userId;
  private boolean hasPicture;
  @Converter(JsonHandler.ListStringHandler.class)
  private List<String> picUrls;
  @Min(value = 1, message = "星级小于1")
  @Max(value = 5, message = "星级大于5")
  private int star;
  @Transient
  private Date createTime;
  @Transient
  @JsonIgnore
  private Date updateTime;
}
