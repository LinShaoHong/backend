package com.github.sun.qm;

import com.github.sun.foundation.mybatis.CompositeMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

@Mapper
public interface PayLogMapper extends CompositeMapper<PayLog> {
    @Select("SELECT * FROM qm_pay_log WHERE userId = #{userId} AND girlId = #{girlId} LIMIT 1")
    PayLog findByUserIdAndGirlId(@Param("userId") String userId, @Param("girlId") String girlId);

    @Select("SELECT SUM(amount) FROM qm_pay_log WHERE userId = #{userId}")
    BigDecimal sumByUserId(@Param("userId") String userId);

    @Select("SELECT COUNT(0) FROM qm_pay_log WHERE userId = #{userId} AND girlId = #{girlId}")
    int countByUserIdAndGirlId(@Param("userId") String userId, @Param("girlId") String girlId);
}
