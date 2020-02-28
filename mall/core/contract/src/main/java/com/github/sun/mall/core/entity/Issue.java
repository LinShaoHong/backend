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
@Table(name = "mall_issue")
public class Issue {
  @Id
  private String id;
  private String question;
  private String answer;
  private boolean deleted;
  @Transient
  private Date createTime;
  @Transient
  private Date updateTime;
}
