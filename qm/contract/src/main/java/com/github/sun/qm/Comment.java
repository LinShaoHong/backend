package com.github.sun.qm;

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
@Table(name = "qm_comment")
public class Comment {
  public static final String SYSTEM = "SYSTEM";

  @Id
  private String id;
  private String sessionId;
  private String girlId;
  private String commentatorId;
  private String replierId;
  private String content;
  private boolean read;
  private boolean privately;
  private long likes;
  private long hates;
  private long time;
  @Transient
  @JsonIgnore
  private Date createTime;
  @Transient
  @JsonIgnore
  private Date updateTime;

  public boolean isSystem() {
    return SYSTEM.equals(commentatorId);
  }
}
