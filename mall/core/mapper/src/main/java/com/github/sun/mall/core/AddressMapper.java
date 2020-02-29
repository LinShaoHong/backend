package com.github.sun.mall.core;

import com.github.sun.foundation.mybatis.CompositeMapper;
import com.github.sun.mall.core.entity.Address;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AddressMapper extends CompositeMapper<Address> {
  @Select("SELECT * FROM mall_address WHERE userId = #{userId}")
  List<Address> findByUserId(@Param("userId") String userId);

  @Select("SELECT * FROM mall_address WHERE id = #{id} AND userId = #{userId}")
  Address findByIdAndUserId(@Param("id") String id, @Param("userId") String userId);

  @Select("SELECT * FROM mall_address WHERE userId = #{userId} AND isDefault = TRUE")
  Address findByUserIdAndIsDefault(@Param("userId") String userId);

  @Update("UPDATE mall_address SET isDefault = FALSE WHERE userId = #{userId} AND isDefault = TRUE")
  void resetDefault(@Param("userId") String userId);
}
