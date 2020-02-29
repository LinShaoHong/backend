package com.github.sun.mall.core;

import com.github.sun.foundation.boot.exception.NotFoundException;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.mall.core.entity.Footprint;
import com.github.sun.mall.core.entity.Goods;
import com.github.sun.mall.core.entity.User;
import com.github.sun.mall.core.resolver.LoginUser;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Builder;
import lombok.Data;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户访问足迹服务
 */
@Path("/v1/mall/footprint")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-core: 访问足迹服务: footprint")
public class FootprintResource extends AbstractResource {
  private final FootprintMapper mapper;
  private final GoodsMapper goodsMapper;

  @Inject
  public FootprintResource(FootprintMapper mapper, GoodsMapper goodsMapper) {
    this.mapper = mapper;
    this.goodsMapper = goodsMapper;
  }

  @GET
  @ApiOperation("用户足迹列表")
  public Object getAll(@QueryParam("start") int start,
                       @QueryParam("count") int count,
                       @Context LoginUser user) {
    int total = mapper.count();
    if (start < total) {
      List<Footprint> footprints = mapper.findPaged(user.getId(), start, count);
      Set<String> goodsIds = footprints.stream().map(Footprint::getGoodsId).collect(Collectors.toSet());
      Map<String, Goods> map = goodsMapper.findByIds(goodsIds).stream().collect(Collectors.toMap(Goods::getId, v -> v));
      List<Resp> result = footprints.stream().map(v -> {
        Goods goods = map.get(v.getGoodsId());
        if (goods != null) {
          return Resp.builder()
            .id(v.getId())
            .goodsId(v.getGoodsId())
            .time(v.getCreateTime().getTime())
            .name(goods.getName())
            .brief(goods.getBrief())
            .picUrl(goods.getPicUrl())
            .retailPrice(goods.getRetailPrice())
            .build();
        }
        return null;
      }).filter(Objects::nonNull).collect(Collectors.toList());
      return responseOf(total, result);
    }
    return responseOf(total, Collections.emptyList());
  }

  @Data
  @Builder
  private static class Resp {
    private String id;
    private String goodsId;
    private long time;
    private String name;
    private String brief;
    private String picUrl;
    private BigDecimal retailPrice;
  }

  @DELETE
  @Path("/${id}")
  @ApiOperation("删除用户足迹")
  public Response delete(@PathParam("id") String id,
                         @Context LoginUser user) {
    Footprint footprint = mapper.findByUserIdAndId(user.getId(), id);
    if (footprint == null) {
      throw new NotFoundException("Can not find footprint by id=" + id);
    }
    mapper.delete(footprint);
    return responseOf();
  }
}
