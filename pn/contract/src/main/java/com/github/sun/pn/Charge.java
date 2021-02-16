package com.github.sun.pn;

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
@Table(name = "pn_charge")
public class Charge {
  public enum Type {
    VIP_MONTH("月VIP"),
    VIP_QUARTER("季VIP"),
    VIP_YEAR("年VIP");

    public final String name;

    Type(String name) {
      this.name = name;
    }
  }

  @Id
  private String id;
  private Type type;
  private String userId;
  private boolean used;
  @Transient
  private Date createTime;
  @Transient
  private Date updateTime;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @NamingStrategy
  @Table(name = "pn_charge_yq")
  public static class YQ {
    @Id
    private Type type;
    private String url;
    private int amount;
  }
}
