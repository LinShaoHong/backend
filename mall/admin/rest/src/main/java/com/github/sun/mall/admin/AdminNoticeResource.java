package com.github.sun.mall.admin;

import com.github.sun.foundation.boot.exception.NotFoundException;
import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.mall.admin.api.AdminNoticeService;
import com.github.sun.mall.admin.auth.Authentication;
import com.github.sun.mall.admin.entity.Admin;
import com.github.sun.mall.admin.entity.Notice;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.*;

@Path("/v1/mall/admin/notice")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-admin: 通知管理: notice")
public class AdminNoticeResource extends AbstractResource {
  private final NoticeMapper mapper;
  private final AdminNoticeService service;
  private final SqlBuilder.Factory factory;
  private final NoticeMapper.Admin noticeAdminMapper;

  @Inject
  public AdminNoticeResource(NoticeMapper mapper,
                             AdminNoticeService service,
                             @Named("mysql") SqlBuilder.Factory factory,
                             NoticeMapper.Admin noticeAdminMapper) {
    this.mapper = mapper;
    this.service = service;
    this.factory = factory;
    this.noticeAdminMapper = noticeAdminMapper;
  }

  @GET
  @ApiOperation("分页查询通知")
  @Authentication(value = "admin:notice:query", tags = {"系统管理", "通知管理", "查询"})
  public PageResponse<Notice> list(@QueryParam("title") String title,
                                   @QueryParam("content") String content,
                                   @QueryParam("start") int start,
                                   @QueryParam("count") int count,
                                   @QueryParam("sort") @DefaultValue("createTime") String sort,
                                   @QueryParam("asc") boolean asc,
                                   @Context Admin admin) {
    SqlBuilder sb = factory.create();
    Expression condition = Expression.nonNull(title).then(sb.field("title").contains(title))
      .and(content == null ? null : sb.field("content").contains(content));
    int total = mapper.countByTemplate(sb.from(Notice.class).where(condition).count().template());
    if (start < total) {
      sb.clear();
      SqlBuilder.Template template = sb.from(Notice.class)
        .where(condition)
        .orderBy(sort, asc)
        .limit(start, count)
        .template();
      final List<Notice> list = mapper.findByTemplate(template);
      return responseOf(total, list);
    }
    return responseOf(total, Collections.emptyList());
  }

  @POST
  @ApiOperation("添加通知")
  @Authentication(value = "admin:notice:create", tags = {"系统管理", "通知管理", "添加"})
  public Response create(@Valid @NotNull(message = "缺少实体") Notice notice,
                         @Context Admin admin) {
    service.create(notice);
    return responseOf();
  }

  @GET
  @Path("/${id}")
  @ApiOperation("获取通知详情")
  @Authentication(value = "admin:notice:detail", tags = {"系统管理", "通知管理", "详情"})
  public Object read(@PathParam("id") String id,
                     @Context Admin admin) {
    Notice notice = mapper.findById(id);
    if (notice == null) {
      throw new NotFoundException("Can not find notice by id=" + id);
    }
    List<Notice.Admin> admins = noticeAdminMapper.findByNoticeId(id);
    Map<String, Object> data = new HashMap<>(2);
    data.put("notice", notice);
    data.put("noticeAdminList", admins);
    return responseOf(data);
  }

  @PUT
  @Path("/${id}")
  @ApiOperation("编辑通知")
  @Authentication(value = "admin:notice:update", tags = {"系统管理", "通知管理", "编辑"})
  public Response update(@PathParam("id") String id,
                         @Valid @NotNull(message = "缺少实体") Notice notice,
                         @Context Admin admin) {
    Notice e = mapper.findById(id);
    if (e == null) {
      throw new NotFoundException("Can not find notice by id=" + id);
    }
    notice.setId(e.getId());
    notice.setAdminId(admin.getId());
    service.update(e, notice);
    return responseOf();
  }

  @DELETE
  @Path("/${id}")
  @ApiOperation("删除通知")
  @Authentication(value = "admin:notice:delete", tags = {"系统管理", "通知管理", "删除"})
  public Response delete(@PathParam("id") String id,
                         @Context Admin admin) {
    service.delete(id);
    return responseOf();
  }

  @DELETE
  @Path("/[{ids}]")
  @ApiOperation("批量删除通知")
  @Authentication(value = "admin:notice:batchDelete", tags = {"系统管理", "通知管理", "批量删除"})
  public Response batchDelete(@PathParam("ids") Set<String> ids,
                              @Context Admin admin) {
    service.batchDelete(ids);
    return responseOf();
  }
}
