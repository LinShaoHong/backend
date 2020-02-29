package com.github.sun.mall.core;

import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.IdGenerator;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.mall.core.entity.Collection;
import com.github.sun.mall.core.entity.Goods;
import com.github.sun.mall.core.resolver.LoginUser;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户收藏服务
 */
@Path("/v1/mall/collection")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-core: 收藏服务: collection")
public class CollectionResource extends AbstractResource {
  private final CollectionMapper mapper;
  private final GoodsMapper goodsMapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public CollectionResource(CollectionMapper mapper, GoodsMapper goodsMapper, SqlBuilder.Factory factory) {
    this.mapper = mapper;
    this.factory = factory;
    this.goodsMapper = goodsMapper;
  }

  @GET
  @ApiOperation("获取用户收藏列表")
  public PageResponse<CollectionResp> getAll(@QueryParam("start") int start,
                                             @QueryParam("count") int count,
                                             @NotNull Byte type,
                                             @QueryParam("sort") @DefaultValue("createTime") String sort,
                                             @QueryParam("asc") boolean asc,
                                             @Context LoginUser user) {
    SqlBuilder sb = factory.create();
    Expression condition = sb.field("userId").eq(user.getId()).and(sb.field("type").eq(type));
    int total = mapper.countByTemplate(sb.from(com.github.sun.mall.core.entity.Collection.class).where(condition).count().template());
    if (start < total) {
      sb.clear();
      SqlBuilder.Template template = sb.from(com.github.sun.mall.core.entity.Collection.class)
        .where(condition)
        .orderBy(sort, asc)
        .limit(start, count)
        .template();
      List<com.github.sun.mall.core.entity.Collection> collections = mapper.findByTemplate(template);
      Set<String> goodsIds = collections.stream().map(com.github.sun.mall.core.entity.Collection::getValueId).collect(Collectors.toSet());
      Map<String, Goods> map = goodsMapper.findByIds(goodsIds).stream().collect(Collectors.toMap(Goods::getId, v -> v));
      List<CollectionResp> result = collections.stream().map(v -> {
        Goods goods = map.get(v.getValueId());
        if (goods != null) {
          return CollectionResp.builder().build();
        }
        return null;
      }).filter(Objects::nonNull).collect(Collectors.toList());
      return responseOf(total, result);
    }
    return responseOf(total, Collections.emptyList());
  }

  @Data
  @Builder
  private static class CollectionResp {
    private String id;
    private int type;
    private String valueId;
    private String name;
    private String brief;
    private String picUrl;
    private String retailPrice;
  }

  /**
   * 如果商品没有收藏，则添加收藏；如果商品已经收藏，则删除收藏状态
   */
  @POST
  @ApiOperation("用户收藏添加或删除")
  public Response add(@Valid @NotNull(message = "缺少实体") Req req, @Context LoginUser user) {
    String userId = user.getId();
    Collection collection = mapper.findByUserIdAndTypeAndValueId(userId, req.getType(), req.getValueId());
    if (collection != null) {
      mapper.delete(collection);
    } else {
      collection = new Collection();
      collection.setId(IdGenerator.next());
      collection.setUserId(userId);
      collection.setValueId(req.getValueId());
      collection.setType(req.getType());
      mapper.insert(collection);
    }
    return responseOf();
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  private static class Req {
    private Collection.Type type;
    @NotNull(message = "缺少商品")
    private String valueId;
  }
}
