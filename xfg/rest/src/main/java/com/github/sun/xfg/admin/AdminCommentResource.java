package com.github.sun.xfg.admin;

import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.xfg.Comment;
import com.github.sun.xfg.CommentMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;

@Path("/v1/xfg/admin/comment")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Admin Comment Resource")
public class AdminCommentResource extends AbstractResource {
  private final CommentMapper mapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public AdminCommentResource(CommentMapper mapper, @Named("mysql") SqlBuilder.Factory factory) {
    this.mapper = mapper;
    this.factory = factory;
  }

  @GET
  @ApiOperation("分页获取评论")
  public PageResponse<Comment> paged(@QueryParam("id") String id,
                                     @QueryParam("girlId") String girlId,
                                     @QueryParam("commentatorId") String commentatorId,
                                     @QueryParam("start") int start,
                                     @QueryParam("count") int count,
                                     @DefaultValue("time") @QueryParam("rank") String rank) {
    SqlBuilder sb = factory.create();
    Expression condition = Expression.id("commentatorId").ne(Comment.SYSTEM)
      .and(id == null ? null : sb.field("id").eq(id))
      .and(girlId == null ? null : sb.field("girlId").eq(girlId))
      .and(commentatorId == null ? null : sb.field("commentatorId").eq(commentatorId));
    int total = mapper.countByTemplate(sb.from(Comment.class).where(condition).count().template());
    if (start < total) {
      sb.clear();
      SqlBuilder.Template template = sb.from(Comment.class)
        .where(condition)
        .desc(rank)
        .limit(start, count)
        .template();
      List<Comment> list = mapper.findByTemplate(template);
      return responseOf(total, list);
    }
    return responseOf(total, Collections.emptyList());
  }

  @DELETE
  @Path("/${id}")
  @ApiOperation("删除")
  public Response delete(@PathParam("id") String id) {
    mapper.deleteById(id);
    return responseOf();
  }
}
