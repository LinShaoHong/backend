package com.github.sun.xzyy;

import com.github.sun.foundation.boot.exception.Message;
import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.xzyy.util.Dates;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Path("/v1/xzyy/comment")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Comment Resource")
@Slf4j
public class CommentResource extends AbstractResource {
  private final CommentMapper mapper;
  private final CommentService service;
  private final UserMapper userMapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public CommentResource(CommentMapper mapper,
                         CommentService service,
                         UserMapper userMapper,
                         @Named("mysql") SqlBuilder.Factory factory) {
    this.mapper = mapper;
    this.service = service;
    this.userMapper = userMapper;
    this.factory = factory;
  }

  @GET
  @ApiOperation("获取评论")
  public SingleResponse<CommentRes> list(@QueryParam("start") int start,
                                         @QueryParam("count") int count,
                                         @QueryParam("commentId") String commentId,
                                         @NotEmpty(message = "缺少评论物品") @QueryParam("girlId") String girlId) {
    SqlBuilder sb = factory.create();
    Expression condition = sb.field("girlId").eq(girlId).and(sb.field("replierId").isNull());
    int total = mapper.countByTemplate(sb.from(Comment.class).where(condition).count().template());
    sb.clear();
    int sum = mapper.countByTemplate(sb.from(Comment.class).where(sb.field("girlId").eq(girlId)).count().template());
    if (commentId != null && !commentId.isEmpty()) {
      Comment comment = mapper.findById(commentId);
      if (comment != null) {
        String id = commentId.equals(comment.getSessionId()) ? commentId : comment.getSessionId();
        int rowNum = mapper.findRowNum(girlId, id);
        start = (rowNum / count) * count;
      }
    }
    if (start < total) {
      sb.clear();
      SqlBuilder.Template template = sb.from(Comment.class)
        .where(condition)
        .desc("time")
        .limit(start, count)
        .template();
      List<Comment> list = mapper.findByTemplate(template);
      List<String> ids = list.stream().map(Comment::getId).collect(Collectors.toList());
      if (!ids.isEmpty()) {
        List<Comment> repliesAll = mapper.findBySessionIdIn(ids).stream()
          .filter(v -> list.stream().noneMatch(c -> c.getId().equals(v.getId())))
          .sorted((r1, r2) -> ((Long) (r2.getTime() - r1.getTime())).intValue())
          .collect(Collectors.toList());
        Set<String> userIds = Stream.concat(list.stream(), repliesAll.stream())
          .flatMap(v -> Stream.of(v.getCommentatorId(), v.getReplierId()))
          .filter(Objects::nonNull)
          .collect(Collectors.toSet());
        Map<String, User> users = userMapper.findByIds(userIds).stream().collect(Collectors.toMap(User::getId, v -> v));
        List<CommentItem> comments = list.stream()
          .map(v -> {
            User user = users.get(v.getCommentatorId());
            List<ReplyRes> replies = repliesAll.stream()
              .filter(r -> r.getSessionId().equals(v.getId()))
              .map(r -> {
                User commentator = users.get(r.getCommentatorId());
                User replier = users.get(r.getReplierId());
                if (commentator != null && replier != null) {
                  return ReplyRes.builder()
                    .id(r.getId())
                    .avatar(commentator.getAvatar())
                    .commentatorId(commentator.getId())
                    .commentatorName(commentator.getUsername())
                    .replierId(replier.getId())
                    .replierName(replier.getUsername())
                    .content(r.getContent())
                    .time(Dates.simpleTime(r.getCreateTime()))
                    .likes(r.getLikes())
                    .hates(r.getHates())
                    .read(r.isRead())
                    .build();
                }
                return null;
              }).filter(Objects::nonNull).collect(Collectors.toList());
            int i;
            for (i = 0; i < replies.size(); i++) {
              if (Objects.equals(replies.get(i).getId(), commentId)) {
                break;
              }
            }
            if (user != null) {
              return CommentItem.builder()
                .id(v.getId())
                .avatar(user.getAvatar())
                .commentatorId(user.getId())
                .commentatorName(user.getUsername())
                .content(v.getContent())
                .time(Dates.simpleTime(v.getCreateTime()))
                .likes(v.getLikes())
                .hates(v.getHates())
                .read(v.isRead())
                .replies(replies)
                .expand(commentId != null && !commentId.isEmpty() && i >= 5 && i < replies.size())
                .build();
            }
            return null;
          }).filter(Objects::nonNull).collect(Collectors.toList());
        return responseOf(CommentRes.builder()
          .sum(sum)
          .start(start)
          .total(total)
          .comments(comments)
          .build());
      }
    }
    return responseOf(CommentRes.builder()
      .sum(sum)
      .start(start)
      .total(total)
      .comments(Collections.emptyList())
      .build());
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  private static class CommentRes {
    private int sum;
    private int start;
    private int total;
    private List<CommentItem> comments;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  private static class CommentItem {
    private String id;
    private String avatar;
    private String commentatorId;
    private String commentatorName;
    private String content;
    private String time;
    private long likes;
    private long hates;
    private boolean expand;
    private boolean read;
    private List<ReplyRes> replies;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  private static class ReplyRes {
    private String id;
    private String avatar;
    private String commentatorId;
    private String commentatorName;
    private String replierId;
    private String replierName;
    private String content;
    private String time;
    private long likes;
    private long hates;
    private boolean read;
  }


  @POST
  @ApiOperation("评论")
  public SingleResponse<String> comment(@Valid @NotNull(message = "require body") CommentReq req,
                                        @Context User user) {
    return responseOf(service.comment(user.getId(), req.getGirlId(), req.getContent()));
  }

  @Data
  private static class CommentReq {
    @NotEmpty(message = "缺少评论物品")
    private String girlId;
    @NotEmpty(message = "缺少评论内容")
    private String content;
  }

  @POST
  @Path("/reply/${id}")
  @ApiOperation("回复")
  public SingleResponse<String> reply(@PathParam("id") String id,
                                      @Valid @NotNull(message = "require body") ReplyReq req,
                                      @Context User user) {
    return responseOf(service.reply(id, req.getContent(), user.getId()));
  }

  @Data
  private static class ReplyReq {
    @NotEmpty(message = "缺少回复内容")
    private String content;
  }

  @GET
  @Path("/reply/message")
  @ApiOperation("获取回复消息")
  public SingleResponse<MessageRes> getReplyInfo(@QueryParam("start") int start,
                                                 @QueryParam("count") int count,
                                                 @QueryParam("isComment") boolean isComment,
                                                 @QueryParam("latestId") String latestId,
                                                 @Context User user) {
    try {
      SqlBuilder sb = factory.create();
      Expression c1 = sb.field("replierId").eq(user.getId()).and(sb.field("commentatorId").ne(Comment.SYSTEM));
      Expression c2 = sb.field("commentatorId").eq(Comment.SYSTEM);
      Expression condition = isComment ? c1 : c2;
      if (latestId != null) {
        Comment latestComment = mapper.findById(latestId);
        if (latestComment == null) {
          return responseOf(MessageRes.builder()
            .total(0)
            .unReads(0)
            .messages(Collections.emptyList()).build());
        }
        condition = condition.and(sb.field("time").gt(latestComment.getTime()));
      }
      int total = latestId != null ? 0 :
        mapper.countByTemplate(sb.from(Comment.class).where(condition).count().template());
      sb.clear();

      int unReads = 0;
      if (latestId == null) {
        Set<String> set = user.getReadSystemMessageIds();
        c1 = sb.field("replierId").eq(user.getId())
          .and(sb.field("commentatorId").ne(Comment.SYSTEM))
          .and(sb.field("read").eq(false));
        c2 = sb.field("commentatorId").eq(Comment.SYSTEM)
          .and(sb.field("read").eq(false))
          .and(sb.field("id").notIn(set));
        Expression conn = isComment ? c1 : c2;
        unReads = mapper.countByTemplate(sb.from(Comment.class)
          .where(conn)
          .count()
          .template());
      }
      if (start < total || latestId != null) {
        sb.clear();
        SqlBuilder.Template template = sb.from(Comment.class)
          .where(condition)
          .desc("time")
          .asc("read")
          .limit(start, count)
          .template();
        List<Comment> comments = mapper.findByTemplate(template);
        Set<String> userIds = comments.stream().map(Comment::getCommentatorId).collect(Collectors.toSet());
        if (!userIds.isEmpty()) {
          Map<String, User> users = userMapper.findByIds(userIds).stream().collect(Collectors.toMap(User::getId, v -> v));
          List<MessageItem> messages = comments.stream()
            .map(v -> {
              User c = users.get(v.getCommentatorId());
              if (v.isSystem()) {
                Set<String> set = user.getReadSystemMessageIds();
                set = set == null ? new HashSet<>() : set;
                boolean read = v.getReplierId() == null ? set.contains(v.getId()) : v.isRead();
                return MessageItem.builder()
                  .id(v.getId())
                  .content(v.getContent())
                  .time(Dates.simpleTime(v.getCreateTime()))
                  .system(true)
                  .read(read)
                  .build();
              }
              if (c != null) {
                return MessageItem.builder()
                  .id(v.getId())
                  .sessionId(v.getSessionId())
                  .girlId(v.getGirlId())
                  .content(v.getContent())
                  .time(Dates.simpleTime(v.getCreateTime()))
                  .commentatorId(c.getId())
                  .avatar(c.getAvatar())
                  .commentatorName(c.getUsername())
                  .replierId(user.getId())
                  .read(v.isRead())
                  .build();
              }
              return null;
            })
            .filter(Objects::nonNull).collect(Collectors.toList());
          return responseOf(MessageRes.builder()
            .total(total)
            .unReads(unReads)
            .messages(messages)
            .build());
        }
      }
      return responseOf(MessageRes.builder()
        .total(total)
        .unReads(unReads)
        .messages(Collections.emptyList()).build());
    } catch (Throwable ex) {
      log.error("Unexpected error", ex);
      throw new Message(10000);
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  private static class MessageRes {
    private int total;
    private int unReads;
    private List<MessageItem> messages;
  }


  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  private static class MessageItem {
    private String id;
    private String sessionId;
    private String girlId;
    private String content;
    private String avatar;
    private String commentatorId;
    private String commentatorName;
    private String replierId;
    private String time;
    private boolean read;
    private boolean system;
  }

  @PUT
  @Path("/read/${id}")
  @ApiOperation("读回复")
  public Response read(@PathParam("id") String id,
                       @Context User user) {
    service.read(user, id);
    return responseOf();
  }

  @PUT
  @Path("/readAll")
  @ApiOperation("全部已读")
  public Response readAll(@Context User user) {
    service.readAll(user);
    return responseOf();
  }

  @PUT
  @Path("/like/${id}")
  @ApiOperation("赞评论")
  public Response like(@PathParam("id") String id) {
    mapper.incLikes(id);
    return responseOf();
  }

  @PUT
  @Path("/hate/${id}")
  @ApiOperation("踩评论")
  public Response hate(@PathParam("id") String id) {
    mapper.incHates(id);
    return responseOf();
  }
}
