package com.github.sun.qm;

import com.github.sun.foundation.mybatis.CompositeMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Date;

@Mapper
public interface FootprintMapper extends CompositeMapper<Footprint> {
  @Update("UPDATE qm_footprint SET updateTime = #{updateTime} WHERE id = #{id}")
  void updateTime(@Param("id") String id, @Param("updateTime") Date updateTime);

  @Insert("INSERT INTO `qm_footprint`(`id`, `userId`, `girlId`) " +
    "VALUES(#{id}, #{userId}, #{girlId}) " +
    "ON DUPLICATE KEY UPDATE `userId` = #{userId}, `girlId` = #{girlId}")
  void insertOrUpdate(Footprint v);
}
