package com.github.sun.picture.mapper;

import com.github.sun.foundation.mybatis.CompositeMapper;
import com.github.sun.picture.Picture;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface PictureDetailsMapper extends CompositeMapper<Picture.Detail> {
  @Select("SELECT * FROM picture_details WHERE `picId` = #{picId}")
  List<Picture.Detail> findByPicId(@Param("picId") String picId);
}
