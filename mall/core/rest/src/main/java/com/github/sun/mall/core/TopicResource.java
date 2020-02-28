package com.github.sun.mall.core;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.github.sun.foundation.boot.exception.NotFoundException;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.mall.core.entity.Goods;
import com.github.sun.mall.core.entity.Topic;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Builder;
import lombok.Data;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

@Path("/v1/mall/topic")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall: Topic Resource", tags = "专题服务")
public class TopicResource extends AbstractResource {
  private final TopicMapper mapper;
  private final GoodsMapper goodsMapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public TopicResource(TopicMapper mapper,
                       GoodsMapper goodsMapper,
                       @Named("mysql") SqlBuilder.Factory factory) {
    this.mapper = mapper;
    this.goodsMapper = goodsMapper;
    this.factory = factory;
  }

  @GET
  @ApiOperation("获取专题列表")
  public PageResponse<Topic> list(@QueryParam("start") int start,
                                  @QueryParam("count") int count,
                                  @QueryParam("sort") @DefaultValue("createTime") String sort,
                                  @QueryParam("asc") boolean asc) {
    int total = mapper.count();
    if (start < total) {
      SqlBuilder sb = factory.create();
      SqlBuilder.Template template = sb.from(Topic.class)
        .orderBy(sort, asc)
        .limit(start, count)
        .template();
      List<Topic> list = mapper.findByTemplate(template);
      return responseOf(total, list);
    }
    return responseOf(total, Collections.emptyList());
  }

  @GET
  @Path("/${id}")
  @ApiOperation("获取单个专题详情")
  public SingleResponse<TopicResp> detail(@PathParam("id") String id) {
    Topic topic = mapper.findById(id);
    if (topic == null) {
      throw new NotFoundException("Can not find topic by id=" + id);
    }
    return responseOf(TopicResp.builder()
      .topic(topic)
      .goods(goodsMapper.findByIds(new HashSet<>(topic.getGoodsIds())))
      .build());
  }

  @Data
  @Builder
  private static class TopicResp {
    @JsonUnwrapped
    private Topic topic;
    private List<Goods> goods;
  }

  @GET
  @Path("/${id}/related")
  @ApiOperation("获取相关专题列表")
  public ListResponse<Topic> related(@PathParam("id") String id,
                                     @QueryParam("start") int start,
                                     @QueryParam("count") @DefaultValue("4") int count) {
    SqlBuilder sb = factory.create();
    SqlBuilder.Template template = sb.from(Topic.class)
      .where(sb.field("id").ne(id))
      .limit(start, count)
      .template();
    List<Topic> list = mapper.findByTemplate(template);
    if (!list.isEmpty()) {
      return responseOf(list);
    }
    return responseOf(list(start, count, "createTime", false).values);
  }
}
