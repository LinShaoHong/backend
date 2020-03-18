package com.github.sun.xzyy;

import com.github.sun.foundation.mybatis.CompositeMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface PayLogMapper extends CompositeMapper<PayLog> {
  @Select("SELECT * FROM xzyy_pay_log WHERE userId = #{userId}")
  List<PayLog> findByUserId(@Param("userId") String userId);

  @Select("SELECT SUM(amount) FROM xzyy_pay_log WHERE userId = #{userId}")
  BigDecimal sumByUserId(@Param("userId") String userId);

  @Select("SELECT COUNT(0) FROM xzyy_pay_log WHERE userId = #{userId} AND girlId = #{girlId}")
  int countByUserIdAndGirlId(@Param("userId") String userId, @Param("girlId") String girlId);
}
