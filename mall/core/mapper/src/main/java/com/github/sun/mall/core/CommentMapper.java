package com.github.sun.mall.core;

import com.github.sun.foundation.mybatis.CompositeMapper;
import com.github.sun.mall.core.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CommentMapper extends CompositeMapper<Comment> {
  @Select("SELECT COUNT(0) FROM mall_comment WHERE type = #{type} AND valueId = #{valueId}")
  int countByTypeAndValueId(@Param("type") Comment.Type type, @Param("valueId") String valueId);

  @Select("SELECT COUNT(0) FROM mall_comment WHERE type = #{type} AND valueId = #{valueId} AND hasPicture = TRUE")
  int countByTypeAndValueIdAndHasPic(@Param("type") Comment.Type type, @Param("valueId") String valueId);
}
