package com.github.sun.xzyy;

import com.github.sun.foundation.boot.exception.Message;
import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.xzyy.resolver.MayLogin;
import com.hankcs.hanlp.HanLP;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Builder;
import lombok.Data;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.sun.foundation.expression.Expression.EMPTY;

@Path("/v1/xzyy/girls")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Girl Resource")
public class GirlResource extends AbstractResource {
  private final GirlMapper mapper;
  private final UserMapper userMapper;
  private final PayLogMapper payLogMapper;
  private final SqlBuilder.Factory factory;
  private final CollectionMapper collectionMapper;
  private final FootprintService footprintService;

  @Inject
  public GirlResource(GirlMapper mapper,
                      UserMapper userMapper,
                      PayLogMapper payLogMapper,
                      @Named("mysql") SqlBuilder.Factory factory,
                      CollectionMapper collectionMapper,
                      FootprintService footprintService) {
    this.mapper = mapper;
    this.userMapper = userMapper;
    this.payLogMapper = payLogMapper;
    this.factory = factory;
    this.collectionMapper = collectionMapper;
    this.footprintService = footprintService;
  }

  @GET
  @ApiOperation("分页获取")
  public PageResponse<GirlResp> paged(@QueryParam("start") int start,
                                      @QueryParam("count") int count,
                                      @QueryParam("type") String type,
                                      @DefaultValue("updateTime") @QueryParam("rank") String rank) {
    SqlBuilder sb = factory.create();
    Expression condition = Expression.nonEmpty(type).then(sb.field("type").eq(type)).and(sb.field("onService").eq(true));
    SqlBuilder.Template template = sb.from(Girl.class)
      .where(condition).count().template();
    int total = mapper.countByTemplate(template);
    if (start < total) {
      sb.clear();
      template = sb.from("xzyy_girl")
        .where(condition)
        .desc(rank)
        .limit(start, count)
        .template();
      List<Girl> girls = mapper.findByTemplate(template);
      return responseOf(total, girls.stream().map(GirlResp::from).collect(Collectors.toList()));
    }
    return responseOf(total, Collections.emptyList());
  }

  @Data
  @Builder
  private static class GirlResp {
    private String id;
    private String name;
    private String city;
    private String mainImage;
    private long likes;
    private long visits;

    private static GirlResp from(Girl v) {
      return GirlResp.builder()
        .id(v.getId())
        .name(v.getName())
        .city(v.getCity())
        .mainImage(v.getMainImage())
        .visits(v.getVisits())
        .likes(v.getLikes())
        .build();
    }
  }

  @GET
  @Path("/hot")
  @ApiOperation("获取热门")
  public ListResponse<GirlResp> host(@QueryParam("count") int count,
                                     @NotNull(message = "缺少type") @QueryParam("type") String type) {
    SqlBuilder sb = factory.create();
    SqlBuilder.Template template = sb.from(Girl.class)
      .where(sb.field("type").eq(type))
      .where(sb.field("visits").gt(0))
      .desc(sb.field("payments").mul(0.4)
        .plus(sb.field("likes").mul(0.3))
        .plus(sb.field("collects").mul(0.2))
        .plus(sb.field("comments").mul(0.2))
        .plus(sb.field("visits").mul(0.1)))
      .limit(count)
      .template();
    List<Girl> girls = mapper.findByTemplate(template);
    girls.sort((g1, g2) -> -g1.getUpdateTime().compareTo(g2.getUpdateTime()));
    return responseOf(girls.stream().map(GirlResp::from).collect(Collectors.toList()));
  }

  @GET
  @Path("/${id}")
  @ApiOperation("看详情")
  public SingleResponse<DetailResp> get(@PathParam("id") String id,
                                        @Context MayLogin mayLogin) {
    Girl girl = mapper.findById(id);
    if (girl == null || !girl.isOnService()) {
      throw new Message(2000);
    }
    SqlBuilder sb = factory.create();
    SqlBuilder.Template template = sb.from(Girl.class)
      .where(sb.field("id").eq(id))
      .update()
      .set("visits", sb.field("visits").plus(1))
      .template();
    mapper.updateByTemplate(template);
    BigDecimal price = girl.getPrice() == null ? new BigDecimal(0) : girl.getPrice();
    boolean accessible = price.compareTo(new BigDecimal(0)) <= 0;
    boolean needCharge = false;
    if (!accessible && mayLogin.user != null) {
      if (mayLogin.user.isVip()) {
        Calendar c = Calendar.getInstance();
        c.setTime(mayLogin.user.getVipEndTime());
        if (c.getTime().compareTo(new Date()) < 0) {
          mayLogin.user.setVip(false);
          userMapper.update(mayLogin.user);
        } else {
          accessible = true;
        }
      }
      if (!accessible) {
        accessible = payLogMapper.countByUserIdAndGirlId(mayLogin.user.getId(), id) > 0;
        if (!accessible) {
          BigDecimal amount = mayLogin.user.getAmount() == null ? new BigDecimal(0) : mayLogin.user.getAmount();
          needCharge = amount.compareTo(price) < 0;
        }
      }
    }
    boolean collected = false;
    if (mayLogin.user != null) {
      collected = collectionMapper.countByUserIdAndGirlId(mayLogin.user.getId(), id) > 0;
    }
    // 记录足迹
    if (mayLogin.user != null) {
      new Thread(() -> footprintService.record(mayLogin.user.getId(), id)).start();
    }
    return responseOf(DetailResp.from(girl, accessible, collected, needCharge));
  }

  @Data
  @Builder
  private static class DetailResp {
    private String id;
    private String name;
    private String city;
    private String title;
    private String contact;
    private String mainImage;
    private BigDecimal price;
    private List<String> detailImages;
    private boolean accessible;
    private boolean collected;
    private boolean needCharge;
    private long likes;
    private long visits;
    private long collects;
    private long comments;
    private boolean onService;

    private static DetailResp from(Girl v, boolean accessible, boolean collected, boolean needCharge) {
      return DetailResp.builder()
        .id(v.getId())
        .name(v.getName())
        .city(v.getCity())
        .title(v.getTitle())
        .price(v.getPrice())
        .contact(accessible ? v.getContact() : null)
        .mainImage(v.getMainImage())
        .detailImages(v.getDetailImages())
        .mainImage(v.getMainImage())
        .visits(v.getVisits())
        .likes(v.getLikes())
        .collects(v.getCollects())
        .comments(v.getComments())
        .accessible(accessible)
        .collected(collected)
        .needCharge(needCharge)
        .onService(v.isOnService())
        .build();
    }
  }

  @PUT
  @Path("/like/${id}")
  @ApiOperation("点赞")
  public Response like(@PathParam("id") String id,
                       @QueryParam("like") boolean like) {
    SqlBuilder sb = factory.create();
    SqlBuilder.Template template = sb.from(Girl.class)
      .where(sb.field("id").eq(id))
      .update()
      .set("likes", like ? sb.field("likes").plus(1) : sb.field("likes").sub(1))
      .template();
    mapper.updateByTemplate(template);
    return responseOf();
  }

  @GET
  @Path("/recommendation/${id}")
  @ApiOperation("获取推荐列表")
  public ListResponse<GirlResp> recommend(@PathParam("id") String id,
                                          @QueryParam("count") int count) {
    Girl girl = mapper.findById(id);
    String type = girl.getType().name();
    SqlBuilder sb = factory.create();
    SqlBuilder.Template template = sb.from(Girl.class)
      .where(sb.field("id").ne(id))
      .where(sb.field("type").eq(type))
      .where(sb.field("onService").eq(true))
      .desc("visits")
      .limit(count)
      .template();
    List<Girl> girls = mapper.findByTemplate(template);
    return responseOf(girls.stream().map(GirlResp::from).collect(Collectors.toList()));
  }

  @GET
  @Path("/search")
  @ApiOperation("搜索")
  public SingleResponse<SearchResp> search(@Pattern(regexp = "COLLECTIVE|INDIVIDUAL|IMAGE|VIDEO") String type,
                                           @NotNull @QueryParam("q") String q) {
    if (q.trim().isEmpty()) {
      return responseOf(SearchResp.builder()
        .girls(Collections.emptyList())
        .keyWords(Collections.emptyList())
        .build());
    }
    List<String> keyWords = HanLP.extractKeyword(q, 10);
    if (keyWords.isEmpty()) {
      keyWords.add(q);
    }
    SqlBuilder sb = factory.create();
    Expression conn = EMPTY;
    for (String word : keyWords) {
      Expression e = sb.field("title").contains(word);
      conn = conn == EMPTY ? e : conn.or(e);
    }
    SqlBuilder.Template template = sb.from(Girl.class)
      .where(conn)
      .desc("visits")
      .template();
    List<Girl> girls = mapper.findByTemplate(template);
    return responseOf(SearchResp.builder()
      .girls(girls.stream().map(GirlResp::from).collect(Collectors.toList()))
      .keyWords(keyWords)
      .build());
  }

  @Data
  @Builder
  private static class SearchResp {
    private List<GirlResp> girls;
    private List<String> keyWords;
  }
}
