package com.github.sun.mall.core;

import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.mall.core.entity.Comment;
import com.github.sun.mall.core.entity.User;
import com.github.sun.mall.core.resolver.LoginUser;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Builder;
import lombok.Data;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Path("/v1/mall/collection")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall: Comment Resource", tags = "评论服务")
public class CommentResource extends AbstractResource {
  private final CommentMapper mapper;
  private final UserMapper userMapper;
  private final GoodsMapper goodsMapper;
  private final TopicMapper topicMapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public CommentResource(CommentMapper mapper,
                         UserMapper userMapper,
                         GoodsMapper goodsMapper,
                         TopicMapper topicMapper,
                         @Named("mysql") SqlBuilder.Factory factory) {
    this.mapper = mapper;
    this.userMapper = userMapper;
    this.goodsMapper = goodsMapper;
    this.topicMapper = topicMapper;
    this.factory = factory;
  }

  @POST
  @ApiOperation("发表评论")
  public Response post(@Valid @NotNull(message = "缺少实体") Comment comment,
                       @Context LoginUser user) {
    String valueId = comment.getValueId();
    Comment.Type type = comment.getType();
    if (type == Comment.Type.GOODS) {
      if (goodsMapper.findById(valueId) == null) {
        throw new NotFoundException("Can not find goods by id=" + valueId);
      }
    } else if (type == Comment.Type.TOPIC) {
      if (topicMapper.findById(valueId) == null) {
        throw new NotFoundException("Can not find topic by id=" + valueId);
      }
    } else {
      throw new BadRequestException("unknown type=" + type);
    }
    if (!comment.isHasPicture()) {
      comment.setPicUrls(Collections.emptyList());
    }
    comment.setUserId(user.getId());
    mapper.insert(comment);
    return responseOf();
  }

  @GET
  @Path("/count")
  @ApiOperation("获取评论数量")
  public SingleResponse<CountResp> count(@QueryParam("type") Comment.Type type,
                                         @NotNull(message = "缺少valueId") String valueId) {
    int allCount = mapper.countByTypeAndValueId(type, valueId);
    int hasPicCount = mapper.countByTypeAndValueIdAndHasPic(type, valueId);
    return responseOf(CountResp.builder()
      .allCount(allCount)
      .hasPicCount(hasPicCount)
      .build());
  }

  @Data
  @Builder
  private static class CountResp {
    private int allCount;
    private int hasPicCount;
  }

  @GET
  @ApiOperation("获取评论列表")
  public Object list(@QueryParam("start") int start,
                     @QueryParam("count") int count,
                     @NotNull(message = "缺少type") @QueryParam("type") Comment.Type type,
                     @NotNull(message = "缺少valueId") @QueryParam("valueId") String valueId,
                     @QueryParam("hasPic") Boolean hasPic) {
    SqlBuilder sb = factory.create();
    Expression condition = sb.field("type").eq(type)
      .and(sb.field("valueId").eq(valueId))
      .and(hasPic == null ? null : sb.field("hasPicture").eq(hasPic));
    int total = mapper.countByTemplate(sb.from(Comment.class).where(condition).count().template());
    if (start < total) {
      sb.clear();
      SqlBuilder.Template template = sb.from(Comment.class)
        .where(condition)
        .limit(start, count)
        .template();
      List<Comment> comments = mapper.findByTemplate(template);
      Set<String> userIds = comments.stream().map(Comment::getUserId).collect(Collectors.toSet());
      Map<String, User> userMap = userMapper.findByIds(userIds).stream().collect(Collectors.toMap(User::getId, v -> v));
      List<CommentResp> result = comments.stream().map(v -> CommentResp.builder()
        .time(v.getCreateTime().getTime())
        .content(v.getContent())
        .adminContent(v.getAdminContent())
        .picUrls(v.getPicUrls())
        .user(UserInfo.from(userMap.get(v.getUserId())))
        .build()).collect(Collectors.toList());
      responseOf(total, result);
    }
    return responseOf(total, Collections.emptyList());
  }

  @Data
  @Builder
  private static class CommentResp {
    private long time;
    private String content;
    private String adminContent;
    private List<String> picUrls;
    private UserInfo user;
  }

  @Data
  @Builder
  private static class UserInfo {
    private String nickName;
    private String avatarUrl;

    static UserInfo from(User u) {
      return UserInfo.builder()
        .nickName(u.getNickname())
        .avatarUrl(u.getAvatar())
        .build();
    }
  }
}
