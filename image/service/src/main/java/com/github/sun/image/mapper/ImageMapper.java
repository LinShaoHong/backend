package com.github.sun.image.mapper;

import com.github.sun.foundation.mybatis.CompositeMapper;
import com.github.sun.image.Image;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ImageMapper extends CompositeMapper<Image> {
  @Mapper
  interface Detail extends CompositeMapper<Image.Detail> {
    @Select("SELECT * FROM `image_details` WHERE `imgId` = #{imgId}")
    List<Image.Detail> findByImgId(@Param("imgId") String imgId);
  }

  @Mapper
  interface Category extends CompositeMapper<Image.Category> {
    @Insert("INSERT INTO `image_category`(`id`, `type`, `label`, `name`, `count`) " +
      "VALUES(#{id}, #{type}, #{label}, #{name}, #{count}) " +
      "ON DUPLICATE KEY UPDATE `count` = `count` + 1")
    void insertOrUpdate(Image.Category category);
  }
}
