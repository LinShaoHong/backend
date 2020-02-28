package com.github.sun.mall.core.entity;

import com.github.sun.foundation.modelling.NamingStrategy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "mall_feedback")
public class Feedback {
  @Id
  private String id;
  private String userId;
  private String username;
  @NotNull(message = "缺少手机号")
  @Pattern(message = "手机号格式不正确", regexp = "^((13[0-9])|(14[5,7])|(15[0-3,5-9])|(16[6])|(17[0,1,3,5-8])|(18[0-9])|(19[8,9]))\\d{8}$")
  private String mobile;
  @NotNull(message = "缺少反馈类型")
  private String type;
  @NotNull(message = "缺少反馈内容")
  private String content;
  private int status;
  private boolean hasPicture;
  private List<String> picUrls;
  private boolean deleted;
  @Transient
  private Date createTime;
  @Transient
  private Date updateTime;
}
