package com.github.sun.qm;

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
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "qm_promotion")
public class Promotion {
  public enum Status {
    PASS, APPROVING, REJECT
  }

  @Id
  private String id;
  private String userId;
  @Converter(JsonHandler.ListStringHandler.class)
  private List<String> images;
  private Status status;
  @Transient
  private Date createTime;
  @Transient
  private Date updateTime;
}
