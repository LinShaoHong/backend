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
import javax.validation.constraints.NotEmpty;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "mall_issue")
public class Issue implements Entity<String> {
  @Id
  private String id;
  @NotEmpty(message = "缺少问题")
  private String question;
  @NotEmpty(message = "缺少回答")
  private String answer;
  @Transient
  @JsonIgnore
  private Date createTime;
  @Transient
  @JsonIgnore
  private Date updateTime;
}
