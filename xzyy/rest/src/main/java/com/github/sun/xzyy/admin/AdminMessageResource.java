package com.github.sun.xzyy.admin;

import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.IdGenerator;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.xzyy.Comment;
import com.github.sun.xzyy.CommentMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;

@Path("/v1/xzyy/admin/message")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Admin Message Resource")
public class AdminMessageResource extends AbstractResource {
  private final CommentMapper mapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public AdminMessageResource(CommentMapper mapper, @Named("mysql") SqlBuilder.Factory factory) {
    this.mapper = mapper;
    this.factory = factory;
  }

  @GET
  @ApiOperation("分页获取消息")
  public PageResponse<Comment> paged(@QueryParam("start") int start,
                                     @QueryParam("count") int count,
                                     @DefaultValue("time") @QueryParam("rank") String rank) {
    SqlBuilder sb = factory.create();
    Expression condition = Expression.id("commentatorId").eq(Comment.SYSTEM)
      .and(sb.field("replierId").isNull());
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

  @POST
  @ApiOperation("添加")
  public Response add(MessageReq req) {
    Comment comment = Comment.builder()
      .id(IdGenerator.next())
      .commentatorId(Comment.SYSTEM)
      .content(req.getContent())
      .time(System.currentTimeMillis())
      .build();
    mapper.insert(comment);
    return responseOf();
  }

  @PUT
  @Path("/${id}")
  @ApiOperation("更新")
  public Response update(@PathParam("id") String id, MessageReq req) {
    Comment comment = mapper.findById(id);
    comment.setContent(req.getContent());
    mapper.update(comment);
    return responseOf();
  }

  @Data
  private static class MessageReq {
    private String content;
  }

  @DELETE
  @Path("/${id}")
  @ApiOperation("删除")
  public Response delete(@PathParam("id") String id) {
    mapper.deleteById(id);
    return responseOf();
  }
}
