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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "mall_collection")
public class Collection implements Entity<String> {
  public enum Type {
    GOODS, TOPIC
  }

  @Id
  private String id;
  private String userId;
  private String valueId;
  private Type type;
  @Transient
  @JsonIgnore
  private Date createTime;
  @Transient
  @JsonIgnore
  private Date updateTime;
}
