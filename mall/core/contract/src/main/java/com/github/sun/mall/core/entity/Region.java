package com.github.sun.mall.core.entity;

import com.github.sun.foundation.modelling.NamingStrategy;
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
@Table(name = "mall_region")
public class Region implements Entity<String> {
  @Id
  private String id;
  private String pId;
  private String name;
  private int type;
  private int code;
}
