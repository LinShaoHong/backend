package com.github.sun.qm.admin;

import com.github.sun.common.EmailSender;
import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.IdGenerator;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.qm.Comment;
import com.github.sun.qm.CommentMapper;
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

@Path("/v1/qm/admin/message")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Admin Message Resource")
public class AdminMessageResource extends AbstractResource {
  private final CommentMapper mapper;
  private final UserMapper userMapper;
  private final EmailSender emailSender;
  private final SqlBuilder.Factory factory;

  @Inject
  public AdminMessageResource(CommentMapper mapper,
                              UserMapper userMapper,
                              @Named("gmail") EmailSender emailSender,
                              @Named("mysql") SqlBuilder.Factory factory) {
    this.mapper = mapper;
    this.userMapper = userMapper;
    this.emailSender = emailSender;
    this.factory = factory;
  }

  @GET
  @ApiOperation("分页获取消息")
  public PageResponse<Comment> paged(@QueryParam("start") int start,
                                     @QueryParam("count") int count,
                                     @DefaultValue("time") @QueryParam("rank") String rank,
                                     @Context Admin admin) {
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
  public Response add(MessageReq req,
                      @Context Admin admin) {
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
  public Response update(@PathParam("id") String id, MessageReq req,
                         @Context Admin admin) {
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
  public Response delete(@PathParam("id") String id,
                         @Context Admin admin) {
    mapper.deleteById(id);
    return responseOf();
  }

  @POST
  @Path("/email")
  @ApiOperation("发送邮件")
  public Response email(EmailReq req, @Context Admin admin) {
    Set<String> emails = Stream.of(req.getTo().replaceAll(" ", "").split(",")).collect(Collectors.toSet());
    if (emails.size() == 1 && emails.iterator().next().equals("@all")) {
      emails = userMapper.findAllEmail();
    }
    if (!emails.isEmpty()) {
      if (req.getFormat().equalsIgnoreCase("HTML")) {
        emailSender.sendHTML("尋芳閣", req.getTitle(), req.getBody(), emails);
      } else {
        emailSender.sendMessage("尋芳閣", req.getTitle(), req.getBody(), emails);
      }
    }
    return responseOf();
  }

  @Data
  private static class EmailReq {
    private String to;
    private String title;
    private String body;
    private String format;
  }
}
