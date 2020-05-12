package com.github.sun.qm.admin;

import com.github.sun.foundation.boot.utility.Pinyins;
import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.IdGenerator;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.qm.Girl;
import com.github.sun.qm.GirlMapper;
import com.github.sun.qm.StorageService;
import com.github.sun.qm.ViewStat;
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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Path("/v1/qm/admin/girl")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Admin Girl Resource")
public class AdminGirlResource extends AbstractResource {
  private final GirlMapper mapper;
  private final StorageService storageService;
  private final GirlMapper.Category categoryMapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public AdminGirlResource(GirlMapper mapper,
                           StorageService storageService,
                           GirlMapper.Category categoryMapper,
                           @Named("mysql") SqlBuilder.Factory factory) {
    this.mapper = mapper;
    this.storageService = storageService;
    this.categoryMapper = categoryMapper;
    this.factory = factory;
  }

  @GET
  @ApiOperation("分页获取")
  public PageResponse<Girl> paged(@QueryParam("id") String id,
                                  @QueryParam("type") String type,
                                  @QueryParam("city") String city,
                                  @QueryParam("name") String name,
                                  @QueryParam("title") String title,
                                  @QueryParam("contact") String contact,
                                  @QueryParam("hasVideo") Boolean hasVideo,
                                  @QueryParam("start") int start,
                                  @QueryParam("count") int count,
                                  @DefaultValue("updateTime") @QueryParam("rank") String rank,
                                  @Context Admin admin) {
    SqlBuilder sb = factory.create();
    Expression condition = Expression.nonEmpty(type).then(sb.field("type").eq(type))
      .and(id == null || id.isEmpty() ? null : sb.field("id").eq(id))
      .and(city == null || city.isEmpty() ? null : sb.field("city").eq(city))
      .and(name == null || name.isEmpty() ? null : sb.field("name").contains(name))
      .and(title == null || title.isEmpty() ? null : sb.field("title").contains(title))
      .and(contact == null || contact.isEmpty() ? null : sb.field("contact").contains(contact))
      .and(hasVideo == null ? null : (hasVideo ? sb.field("videos").isNotNull() : sb.field("videos").isNull()));
    int total = mapper.countByTemplate(sb.from(Girl.class).where(condition).count().template());
    if (start < total) {
      sb.clear();
      SqlBuilder.Template template = sb.from(Girl.class)
        .where(condition)
        .desc(rank)
        .limit(start, count)
        .template();
      final List<Girl> list = mapper.findByTemplate(template);
      return responseOf(total, list);
    }
    return responseOf(total, Collections.emptyList());
  }

  @POST
  @ApiOperation("添加")
  public Response create(@Valid @NotNull(message = "缺少实体") Girl v,
                         @Context Admin admin) {
    v.setId(IdGenerator.next());
    if (v.getCategory() != null) {
      v.setCategorySpell(Stream.of(v.getCategory().split("\\|")).map(Pinyins::spell).collect(Collectors.joining("|")));
      updateCategory(v);
    }
    mapper.insert(v);
    if (v.getCategory() != null && !v.getCategory().isEmpty()) {
      updateCategory(v);
    }
    return responseOf();
  }

  private void updateCategory(Girl v) {
    String tags = v.getCategory();
    if (tags != null && !tags.isEmpty()) {
      List<Girl.Category> categories = Arrays.stream(tags.split("\\|"))
        .distinct()
        .map(name -> {
          String nameSpell = Pinyins.spell(name);
          return Girl.Category.builder()
            .id(v.getType().name() + ":" + nameSpell)
            .type(v.getType())
            .name(name)
            .nameSpell(nameSpell)
            .count(1)
            .build();
        })
        .collect(Collectors.toList());
      categories.forEach(categoryMapper::insertOrUpdate);
    }
  }

  @GET
  @Path("/${id}")
  @ApiOperation("详情")
  public SingleResponse<Girl> create(@PathParam("id") String id,
                                     @Context Admin admin) {
    return responseOf(mapper.findById(id));
  }

  @PUT
  @Path("/unShelve/${id}")
  @ApiOperation("下架")
  public Response unShelve(@PathParam("id") String id,
                           @Context Admin admin) {
    Girl girl = mapper.findById(id);
    if (girl != null && girl.isOnService()) {
      girl.setOnService(false);
      mapper.update(girl);
    }
    return responseOf();
  }

  @DELETE
  @Path("/${id}")
  @ApiOperation("删除")
  public Response delete(@PathParam("id") String id,
                         @Context Admin admin) {
    Girl girl = mapper.findById(id);
    if (girl != null && girl.isOnService()) {
      if (girl.getMainImage() != null) {
        storageService.delete(girl.getMainImage());
      }
      girl.getDetailImages().forEach(storageService::delete);
      mapper.deleteById(id);
    }
    return responseOf();
  }

  @PUT
  @Path("/publish/${id}")
  @ApiOperation("上课")
  public Response publish(@PathParam("id") String id,
                          @Context Admin admin) {
    Girl girl = mapper.findById(id);
    if (girl != null && !girl.isOnService()) {
      girl.setOnService(true);
      mapper.update(girl);
    }
    return responseOf();
  }

  @PUT
  @Path("/${id}")
  @ApiOperation("修改")
  public Response create(@PathParam("id") String id,
                         @Valid @NotNull(message = "缺少实体") Girl v,
                         @Context Admin admin) {
    Girl e = mapper.findById(id);
    if (e.getCategory() != null) {
      categoryMapper.dec(v.getType().name() + ":" + v.getCategorySpell());
    }
    v.setId(id);
    mapper.update(v);
    updateCategory(v);
    return responseOf();
  }

  private final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd");

  @GET
  @Path("/stat")
  @ApiOperation("统计访问量")
  public SingleResponse<StatResp> statUser(@QueryParam("timeType") int timeType,
                                           @QueryParam("type") String type,
                                           @Context Admin admin) {
    SqlBuilder sb = factory.create();
    Calendar c = Calendar.getInstance();
    Date now = new Date();
    c.setTime(now);
    switch (timeType) {
      case 1:
        c.add(Calendar.DAY_OF_WEEK, -7);
        break;
      case 2:
        c.add(Calendar.MONTH, -1);
        break;
      case 3:
        c.add(Calendar.MONTH, -3);
        break;
      case 4:
        c.add(Calendar.MONTH, -6);
        break;
      case 5:
        c.add(Calendar.YEAR, -1);
        break;
    }
    SqlBuilder.Template template = sb.from(ViewStat.class)
      .where(sb.field("date").ge(FORMATTER.format(c.getTime())))
      .where(sb.field("type").eq(type))
      .select(sb.field("type"), "type")
      .select(sb.field("date"), "time")
      .select(sb.field("visits"), "count")
      .template();
    List<Map<String, Object>> list = mapper.findByTemplateAsMap(template);
    List<String> times = new ArrayList<>();
    List<Integer> visits = new ArrayList<>();
    for (Map<String, Object> map : list) {
      times.add((String) map.get("time"));
      Integer inc = ((Long) map.get("count")).intValue();
      visits.add(inc);
    }
    return responseOf(StatResp.builder()
      .times(times)
      .visits(visits)
      .build());
  }

  @Data
  @Builder
  private static class StatResp {
    private List<String> times;
    private List<Integer> visits;
  }
}
