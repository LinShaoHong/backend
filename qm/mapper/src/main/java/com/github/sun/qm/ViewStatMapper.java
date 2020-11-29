package com.github.sun.qm;

import com.github.sun.foundation.mybatis.CompositeMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ViewStatMapper extends CompositeMapper<ViewStat> {
  @Insert("INSERT INTO `qm_view_stat`(`id`, `type`, `date`, `visits`) " +
    "VALUES(#{id}, #{type}, #{date}, #{visits}) " +
    "ON DUPLICATE KEY UPDATE `visits` = `visits` + 1")
  void insertOrUpdate(ViewStat v);

  @Select("SELECT SUBSTRING_INDEX(SUBSTRING_INDEX(`id`, ':', -2), ':', 1) AS city, " +
    "SUM(`visits`) AS count " +
    "FROM `qm_view_stat` " +
    "WHERE `type` = #{type} AND `date` = #{date} " +
    "GROUP BY `id`")
  List<Map<String, Object>> sum(@Param("type") String type, @Param("date") String date);

  @Select("SELECT SUBSTRING_INDEX(`id`, ':', -1) AS date," +
    "SUBSTRING_INDEX(SUBSTRING_INDEX(`id`, ':', -2), ':', 1) AS city," +
    "SUM(`visits`) AS count " +
    "FROM `qm_view_stat` " +
    "WHERE `type` = #{type} AND `date` >= #{startTime} " +
    "GROUP BY `id`")
  List<Map<String, Object>> stat(@Param("startTime") String startTime, @Param("type") String type);
}
