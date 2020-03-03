package com.github.sun.mall.admin;

import com.github.sun.foundation.mybatis.CompositeMapper;
import com.github.sun.foundation.mybatis.interceptor.anno.Flatten;
import com.github.sun.mall.admin.entity.Notice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;
import java.util.Set;

@Mapper
public interface NoticeMapper extends CompositeMapper<Notice> {
  @Mapper
  interface Admin extends CompositeMapper<Notice.Admin> {
    @Select("SELECT COUNT(0) FROM mall_admin_notice_admin WHERE adminId = #{adminId}")
    int countByAdminId(@Param("adminId") String adminId);

    @Select("SELECT * FROM mall_admin_notice_admin WHERE noticeId = #{noticeId}")
    List<Notice.Admin> findByNoticeId(@Param("noticeId") String noticeId);

    @Select("SELECT COUNT(0) FROM mall_admin_notice_admin WHERE noticeId = #{noticeId} AND readTime IS NOT NULL")
    int countByNoticeIdAndReadTimeNotNull(@Param("noticeId") String noticeId);

    @Update("UPDATE mall_admin_notice_admin SET updateTime = #{updateTime} WHERE noticeId = #{noticeId}")
    void updateUpdateTimeByNoticeId(@Param("noticeId") String noticeId, @Param("updateTime") Date updateTime);

    @Select("DELETE FROM mall_admin_notice_admin WHERE noticeId = #{noticeId}")
    void deleteByNoticeId(@Param("noticeId") String noticeId);

    @Select("DELETE FROM mall_admin_notice_admin WHERE noticeId IN(${noticeId})")
    void deleteByNoticeIdIn(@Flatten @Param("noticeIds") Set<String> noticeIds);
  }
}
