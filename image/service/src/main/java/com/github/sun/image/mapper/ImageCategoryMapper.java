package com.github.sun.image.mapper;

import com.github.sun.foundation.mybatis.CompositeMapper;
import com.github.sun.image.Image;
import org.apache.ibatis.annotations.Insert;

public interface ImageCategoryMapper extends CompositeMapper<Image.Category> {
  @Insert("INSERT INTO `image_category`(`id`, `type`, `label`, `name`, `count`) " +
    "VALUES(#{id}, #{type}, #{label}, #{name}, #{count}) " +
    "ON DUPLICATE KEY UPDATE `count` = `count` + 1")
  void insertOrUpdate(Image.Category category);
}
