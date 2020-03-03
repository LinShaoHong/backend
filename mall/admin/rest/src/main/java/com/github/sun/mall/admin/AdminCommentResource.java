package com.github.sun.mall.admin;

import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.mall.admin.auth.Authentication;
import com.github.sun.mall.admin.entity.Admin;
import com.github.sun.mall.core.CommentMapper;
import com.github.sun.mall.core.entity.Comment;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;

@Path("/v1/mall/admin/comment")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-admin: 评价管理: comment")
public class AdminCommentResource extends AbstractResource {
  private final CommentMapper mapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public AdminCommentResource(CommentMapper mapper, @Named("mysql") SqlBuilder.Factory factory) {
    this.mapper = mapper;
    this.factory = factory;
  }

  @GET
  @ApiOperation("分页获取评价列表")
  @Authentication(value = "admin:comment:query", tags = {"商品管理", "评价管理", "查询"})
  public PageResponse<Comment> getAll(@QueryParam("userId") String userId,
                                      @QueryParam("valueId") String valueId,
                                      @QueryParam("start") int start,
                                      @QueryParam("count") int count,
                                      @QueryParam("sort") @DefaultValue("createTime") String sort,
                                      @QueryParam("asc") boolean asc,
                                      @Context Admin admin) {
    SqlBuilder sb = factory.create();
    Expression condition = Expression.nonNull(userId).then(sb.field("userId").eq(userId))
      .and(valueId == null ? null : sb.field("valueId").contains(valueId).and(sb.field("type").eq(Comment.Type.GOODS.name())))
      .and(sb.field("type").ne(Comment.Type.ORDER.name()));
    int total = mapper.countByTemplate(sb.from(Comment.class).where(condition).count().template());
    if (start < total) {
      SqlBuilder.Template template = sb.from(Comment.class)
        .where(condition)
        .orderBy(sort, asc)
        .limit(start, count)
        .template();
      final List<Comment> list = mapper.findByTemplate(template);
      return responseOf(total, list);
    }
    return responseOf(total, Collections.emptyList());
  }
}
