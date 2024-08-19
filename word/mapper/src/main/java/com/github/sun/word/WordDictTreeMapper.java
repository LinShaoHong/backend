package com.github.sun.word;

import com.github.sun.foundation.mybatis.CompositeMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WordDictTreeMapper extends CompositeMapper<WordDictTree> {
  @Select("select * from word_dict_tree where root=#{root} order by createTime desc")
  List<WordDictTree> byRoot(@Param("root") String root);

  @Select("select * from word_dict_tree where root=#{root} and rootDesc=#{desc}")
  WordDictTree byRootAndDesc(@Param("root") String root, @Param("desc") String desc);

  @Select("select * from word_dict_tree where JSON_CONTAINS(derivatives,#{word});")
  List<WordDictTree> findTree(@Param("word") String word);
}