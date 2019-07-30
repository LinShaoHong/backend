package com.github.sun.image.mapper;

import com.github.sun.foundation.mybatis.CompositeMapper;
import com.github.sun.image.Image;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ImageDetailsMapper extends CompositeMapper<Image.Detail> {
  @Select("SELECT * FROM image_details WHERE `imgId` = #{imgId}")
  List<Image.Detail> findByImgId(@Param("imgId") String imgId);
}
