package com.github.sun.qm.admin;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.sql.IdGenerator;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.qm.Comment;
import com.github.sun.qm.CommentMapper;
import com.github.sun.qm.GirlMapper;
import com.github.sun.qm.UserMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Path("/v1/qm/admin/comment")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Admin Comment Resource")
public class AdminCommentResource extends AdminBasicResource {
  private final CommentMapper mapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public AdminCommentResource(UserMapper userMapper,
                              GirlMapper girlMapper,
                              CommentMapper mapper,
                              @Named("mysql") SqlBuilder.Factory factory) {
    super(userMapper, girlMapper);
    this.mapper = mapper;
    this.factory = factory;
  }

  @GET
  @ApiOperation("分页获取评论")
  public PageResponse<ObjectNode> paged(@QueryParam("id") String id,
                                        @QueryParam("commentatorName") String commentatorName,
                                        @QueryParam("replierName") String replierName,
                                        @QueryParam("girlName") String girlName,
                                        @QueryParam("system") boolean system,
                                        @QueryParam("start") int start,
                                        @QueryParam("count") int count,
                                        @QueryParam("rank") @DefaultValue("time") String rank,
                                        @Context Admin admin) {
    String commentatorId = null;
    if (commentatorName != null && !commentatorName.isEmpty()) {
      commentatorId = userMapper.findIdByUsername(commentatorName);
    }
    String replierId = null;
    if (replierName != null && !replierName.isEmpty()) {
      replierId = userMapper.findIdByUsername(replierName);
    }
    Set<String> girlIds = null;
    if (girlName != null && !girlName.isEmpty()) {
      girlIds = girlMapper.findIdsByName(girlName);
    }
    SqlBuilder sb = factory.create();
    Expression condition = (
      system ?
        (Expression.id("commentatorId").eq(Comment.SYSTEM)
          .and(sb.field("replierId").isNotNull())
          .and(sb.field("content").contains("恭喜您注册成功").not())) :
        (Expression.id("commentatorId").ne(Comment.SYSTEM))
    ).and(id == null ? null : sb.field("id").eq(id))
      .and(girlIds == null || girlIds.isEmpty() ? null : sb.field("girlId").in(girlIds))
      .and(replierId == null ? null : sb.field("replierId").eq(replierId))
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
      return responseOf(total, join(list, "commentatorId", "replierId", "girlId"));
    }
    return responseOf(total, Collections.emptyList());
  }

  @DELETE
  @Path("/${id}")
  @ApiOperation("删除")
  public Response delete(@PathParam("id") String id,
                         @Context Admin admin) {
    mapper.deleteById(id);
    return responseOf();
  }

  @PUT
  @Path("/private/${id}")
  @ApiOperation("禁开")
  public Response privately(@PathParam("id") String id,
                            @Context Admin admin) {
    mapper.publicity(id);
    return responseOf();
  }

  @POST
  @ApiOperation("通知")
  public Response notice(NoticeReq req, @Context Admin admin) {
    Set<String> userIds = Stream.of(req.getUsername().replaceAll(" ", "").split(",")).collect(Collectors.toSet());
    if (userIds.size() == 1 && userIds.iterator().next().equals("@all")) {
      userIds = userMapper.findAllIds();
    } else {
      userIds.clear();
      String id = userMapper.findIdByUsername(req.getUsername());
      if (id != null) {
        userIds.add(id);
      }
    }
    List<Comment> comments = userIds.stream().map(id -> Comment.builder()
      .id(IdGenerator.next())
      .girlId(req.getGirlId())
      .commentatorId(Comment.SYSTEM)
      .replierId(id)
      .time(System.currentTimeMillis())
      .content(req.getContent())
      .build()).collect(Collectors.toList());
    if (!comments.isEmpty()) {
      mapper.insertAll(comments);
    }
    return responseOf();
  }

  @Data
  private static class NoticeReq {
    private String username;
    private String content;
    private String girlId;
  }
}
