package com.github.sun.qm;

import com.github.sun.foundation.boot.utility.Dates;
import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.SqlBuilder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.*;
import java.util.stream.Collectors;

@Path("/v1/qm/footprint")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Footprint Resource")
public class FootprintResource extends AbstractResource {
  private final FootprintMapper mapper;
  private final GirlMapper girlMapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public FootprintResource(FootprintMapper mapper,
                           GirlMapper girlMapper,
                           @Named("mysql") SqlBuilder.Factory factory) {
    this.mapper = mapper;
    this.girlMapper = girlMapper;
    this.factory = factory;
  }

  @GET
  @ApiOperation("获取我的足迹")
  public PageResponse<FootprintRes> list(@QueryParam("start") int start,
                                         @QueryParam("count") int count,
                                         @Context User user) {
    SqlBuilder sb = factory.create();
    Expression condition = sb.field("userId").eq(user.getId());
    int total = mapper.countByTemplate(sb.from(Footprint.class).where(condition).count().template());
    if (start < total) {
      SqlBuilder.Template template = sb.from(Footprint.class)
        .where(condition)
        .desc("updateTime")
        .limit(start, count)
        .template();
      List<Footprint> list = mapper.findByTemplate(template);
      Set<String> girlIds = list.stream().map(Footprint::getGirlId).collect(Collectors.toSet());
      Map<String, Girl> girls = girlMapper.findByIds(girlIds).stream().collect(Collectors.toMap(Girl::getId, v -> v));
      return responseOf(total, list.stream().map(v -> {
        Girl g = girls.get(v.getGirlId());
        if (g != null) {
          return FootprintRes.builder()
            .id(v.getId())
            .girlId(g.getId())
            .name(g.getName())
            .city(g.getCity())
            .type(g.getType())
            .mainImage(g.getMainImage())
            .time(Dates.simpleTime(v.getUpdateTime()))
            .onService(g.isOnService())
            .build();
        }
        return null;
      }).filter(Objects::nonNull).collect(Collectors.toList()));
    }
    return responseOf(total, Collections.emptyList());
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  private static class FootprintRes {
    private String id;
    private String girlId;
    private String name;
    private String city;
    private Girl.Type type;
    private String mainImage;
    private String time;
    private boolean onService;
  }
}
