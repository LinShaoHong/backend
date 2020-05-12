package com.github.sun.qm;

import com.github.sun.foundation.mybatis.CompositeMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PromotionMapper extends CompositeMapper<Promotion> {
  @Select("SELECT * FROM qm_promotion WHERE userId = #{userId} ORDER BY createTime DESC")
  List<Promotion> findByUserId(@Param("userId") String userId);
}
