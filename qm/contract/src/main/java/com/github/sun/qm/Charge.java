package com.github.sun.qm;

import com.github.sun.foundation.modelling.NamingStrategy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.math.BigDecimal;
import java.util.Date;

import static com.github.sun.qm.Charge.Type.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "qm_charge")
public class Charge {
  public enum Type {
    TEN(new BigDecimal("10")),
    THIRTY(new BigDecimal("30")),
    FIFTY(new BigDecimal("50")),
    VIP_MONTH(new BigDecimal("68")),
    VIP_QUARTER(new BigDecimal("128")),
    VIP_YEAR(new BigDecimal("258")),
    VIP_FOREVER(new BigDecimal("398"));

    public final BigDecimal price;

    Type(BigDecimal price) {
      this.price = price;
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

  public boolean isVip() {
    return type == VIP_MONTH ||
      type == VIP_QUARTER ||
      type == VIP_YEAR ||
      type == VIP_FOREVER;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @NamingStrategy
  @Table(name = "qm_charge_yq")
  public static class YQ {
    @Id
    private Type type;
    private String url;
    private int amount;
  }
}
