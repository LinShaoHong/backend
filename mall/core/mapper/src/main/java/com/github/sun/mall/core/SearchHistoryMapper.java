package com.github.sun.mall.core;

import com.github.sun.foundation.mybatis.CompositeMapper;
import com.github.sun.mall.core.entity.SearchHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SearchHistoryMapper extends CompositeMapper<SearchHistory> {
  @Select("SELECT * FROM mall_search_history WHERE userId = #{userId}")
  List<SearchHistory> findByUserId(@Param("userId") String userId);

  @Select("DELETE FROM mall_search_history WHERE userId = #{userId}")
  List<SearchHistory> deleteByUserId(@Param("userId") String userId);
}
