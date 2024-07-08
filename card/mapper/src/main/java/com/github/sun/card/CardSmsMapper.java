package com.github.sun.card;

import com.github.sun.foundation.mybatis.CompositeMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CardSmsMapper extends CompositeMapper<CardSms> {
  @Select("select * from card_sms where userId=#{userId}")
  List<CardSms> byUserId(@Param("userId") String userId);

  @Select("select * from card_sms where toPhone=#{toPhone}")
  List<CardSms> byToPhone(@Param("toPhone") String toPhone);

  @Select("select * from card_sms where fromPhone=#{fromPhone} and toPhone=#{toPhone}")
  List<CardSms> byFromPhoneAndToPhone(@Param("fromPhone") String fromPhone,
                                      @Param("toPhone") String toPhone);
}