package com.github.sun.qm;

import com.github.sun.foundation.mybatis.CompositeMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

@Mapper
public interface ChargeMapper extends CompositeMapper<Charge> {
  @Select("SELECT SUM(amount) FROM sun.qm_pay_log WHERE type = 'RECHARGE'")
  BigDecimal rechargeTotal();

  @Mapper
  interface YQMapper extends CompositeMapper<Charge.YQ> {
  }
}
