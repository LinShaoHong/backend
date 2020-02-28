package com.github.sun.mall.core.entity;

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
@Table(name = "mall_log")
public class Log {
  @Id
  private String id;
  private String admin;
  private String ip;
  private int type;
  private String action;
  private boolean status;
  private String result;
  private String comment;
  private boolean deleted;
  @Transient
  private Date createTime;
  @Transient
  private Date updateTime;
}
