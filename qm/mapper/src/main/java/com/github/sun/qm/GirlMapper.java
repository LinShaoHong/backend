package com.github.sun.qm;

import com.github.sun.foundation.mybatis.CompositeMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GirlMapper extends CompositeMapper<Girl> {
  @Mapper
  interface Category extends CompositeMapper<Girl.Category> {
    @Insert("INSERT INTO `qm_girl_category`(`id`, `type`, `name`, `nameSpell`, `count`) " +
      "VALUES(#{id}, #{type}, #{name}, #{nameSpell}, #{count}) " +
      "ON DUPLICATE KEY UPDATE `count` = `count` + 1")
    void insertOrUpdate(Girl.Category category);

    @Insert("UPDATE `qm_girl_category` SET count = count - 1 WHERE id = #{id}")
    void dec(@Param("id") String id);
  }
}
