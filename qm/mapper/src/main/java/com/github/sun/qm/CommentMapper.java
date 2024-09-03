package com.github.sun.qm;

import com.github.sun.foundation.mybatis.CompositeMapper;
import com.github.sun.foundation.mybatis.interceptor.anno.Flatten;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Set;

@Mapper
public interface CommentMapper extends CompositeMapper<Comment> {
    @Select("SELECT * FROM qm_comment WHERE sessionId IN(${sessionIds})")
    List<Comment> findBySessionIdIn(@Flatten @Param("sessionIds") List<String> sessionIds);

    @Update("UPDATE qm_comment SET likes = likes + 1 WHERE id = #{id}")
    void incLikes(@Param("id") String id);

    @Update("UPDATE qm_comment SET hates = hates + 1 WHERE id = #{id}")
    void incHates(@Param("id") String id);

    @Update("UPDATE qm_comment SET `read` = TRUE WHERE id = #{id}")
    void read(@Param("id") String id);

    @Update("UPDATE qm_comment SET `read` = TRUE WHERE replierId = #{userId}")
    void readAll(@Param("userId") String userId);

    @Update("UPDATE qm_comment SET `privately` = TRUE WHERE id = #{id}")
    void publicity(@Param("id") String id);

    @Select("SELECT id FROM qm_comment WHERE commentatorId = 'SYSTEM' AND replierId IS NULL")
    Set<String> findAllSystemMessageId();

    @Select("select m.rowNum from " +
            "(SELECT @rownum := @rownum + 1 as rowNum, e.* FROM (SELECT @rownum := 0) r, qm_comment e " +
            "WHERE e.girlId = #{girlId} AND e.privately = FALSE AND e.replierId IS NULL OR (e.commentatorId = #{userId} AND e.privately = TRUE) ORDER BY e.createTime DESC) m " +
            "WHERE m.id = #{id}")
    int findRowNumByUserId(@Param("userId") String userId, @Param("girlId") String girlId, @Param("id") String id);

    @Select("select m.rowNum from " +
            "(SELECT @rownum := @rownum + 1 as rowNum, e.* FROM (SELECT @rownum := 0) r, qm_comment e " +
            "WHERE e.girlId = #{girlId} AND e.privately = FALSE AND e.replierId IS NULL ORDER BY e.createTime DESC) m " +
            "WHERE m.id = #{id}")
    int findRowNum(@Param("girlId") String girlId, @Param("id") String id);
}
