package com.github.sun.xzyy;

import com.github.sun.foundation.modelling.NamingStrategy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;

import static com.github.sun.xzyy.Charge.Type.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "xzyy_charge")
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

  public boolean isVip() {
    return type == VIP_MONTH ||
      type == VIP_QUARTER ||
      type == VIP_YEAR ||
      type == VIP_FOREVER;
  }
}
