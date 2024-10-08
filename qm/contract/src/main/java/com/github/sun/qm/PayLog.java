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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "qm_pay_log")
public class PayLog {
    public enum Type {
        SING_IN, RECHARGE, PAYMENT, PROMOTION
    }

    @Id
    private String id;
    private String userId;
    private BigDecimal amount;
    private Type type;
    private String chargeType;
    private String girlId;
    @Transient
    private Date createTime;
    @Transient
    private Date updateTime;
}
