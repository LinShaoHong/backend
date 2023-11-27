package com.github.sun.qm.admin;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.sun.foundation.boot.utility.Iterators;
import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.foundation.boot.utility.Pinyins;
import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.IdGenerator;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.qm.Girl;
import com.github.sun.qm.GirlMapper;
import com.github.sun.qm.StorageService;
import com.github.sun.qm.ViewStatMapper;
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
  private final ViewStatMapper viewStatMapper;
  private final StorageService storageService;
  private final GirlMapper.Category categoryMapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public AdminGirlResource(GirlMapper mapper,
                           ViewStatMapper viewStatMapper, StorageService storageService,
                           GirlMapper.Category categoryMapper,
                           @Named("mysql") SqlBuilder.Factory factory) {
    this.mapper = mapper;
    this.viewStatMapper = viewStatMapper;
    this.storageService = storageService;
    this.categoryMapper = categoryMapper;
    this.factory = factory;
  }

  @GET
  @ApiOperation("分页获取")
  public PageResponse<ObjectNode> paged(@QueryParam("id") String id,
                                        @QueryParam("type") String type,
                                        @QueryParam("city") String city,
                                        @QueryParam("name") String name,
                                        @QueryParam("title") String title,
                                        @QueryParam("contact") String contact,
                                        @QueryParam("hasVideo") Boolean hasVideo,
                                        @QueryParam("onService") Boolean onService,
                                        @QueryParam("start") int start,
                                        @QueryParam("count") int count,
                                        @QueryParam("rank") @DefaultValue("updateTime") String rank,
                                        @Context Admin admin) {
    SqlBuilder sb = factory.create();
    Expression condition = Expression.nonEmpty(type).then(sb.field("type").eq(type))
      .and(id == null || id.isEmpty() ? null : sb.field("id").eq(id))
      .and(city == null || city.isEmpty() ? null : sb.field("city").eq(city))
      .and(name == null || name.isEmpty() ? null : sb.field("name").contains(name))
      .and(title == null || title.isEmpty() ? null : sb.field("title").contains(title))
      .and(contact == null || contact.isEmpty() ? null : sb.field("contact").contains(contact))
      .and(onService == null ? null : sb.field("onService").eq(onService))
      .and(hasVideo == null ? null : (hasVideo ? sb.field("json_length").call(sb.field("videos")).gt(0) : sb.field("videos").isNull()));
    int total = mapper.countByTemplate(sb.from(Girl.class).where(condition).count().template());
    if (start < total) {
      sb.clear();
      SqlBuilder.Template template = sb.from(Girl.class)
        .where(condition)
        .desc(rank)
        .limit(start, count)
        .template();
      final List<Girl> list = mapper.findByTemplate(template);
      return responseOf(total, JSON.deserializeAsList(list, ObjectNode.class));
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
      girl.getContactImages().forEach(storageService::delete);
      girl.getVideos().forEach(storageService::delete);
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
  public ListResponse<StatResp> statUser(@QueryParam("timeType") int timeType,
                                         @QueryParam("type") String type,
                                         @Context Admin admin) {
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
    List<Map<String, Object>> result = viewStatMapper.stat(FORMATTER.format(c.getTime()), type);
    Map<String, Integer> total = new LinkedHashMap<>();
    List<StatResp> ret = result.stream().collect(Collectors.groupingBy(v -> v.get("city")))
      .entrySet()
      .stream()
      .map(e -> {
        String city = (String) e.getKey();
        List<Map<String, Object>> list = e.getValue();
        List<String> times = new ArrayList<>();
        List<Integer> visits = new ArrayList<>();
        for (Map<String, Object> map : list) {
          String date = (String) map.get("date");
          times.add(date);
          int inc = ((Long) map.get("visits")).intValue();
          visits.add(inc);
          inc += total.computeIfAbsent(date, d -> 0);
          total.put(date, inc);
        }
        return StatResp.builder()
          .city(city)
          .times(times)
          .visits(visits)
          .build();
      }).collect(Collectors.toList());
    if (ret.size() > 1) {
      ret.removeIf(v -> "TOTAL".equals(v.getCity()));
      List<String> times = new ArrayList<>();
      List<Integer> visits = new ArrayList<>();
      total.forEach((key, value) -> {
        times.add(key);
        visits.add(value);
      });
      int len = times.size();
      ret.forEach(v -> {
        List<String> oldTimes = v.getTimes();
        List<Integer> oldVisits = v.getVisits();

        List<String> newTimes = new ArrayList<>(times.size());
        Iterators.slice(times.size()).forEach(j -> newTimes.add(""));

        List<Integer> newVisits = new ArrayList<>(visits.size());
        Iterators.slice(visits.size()).forEach(j -> newVisits.add(0));

        v.setTimes(newTimes);
        v.setVisits(newVisits);

        for (int i = 0; i < len; i++) {
          String date = times.get(i);
          v.getTimes().set(i, date);
          if (oldTimes.stream().anyMatch(t -> t.equals(date))) {
            int j = oldTimes.indexOf(date);
            v.getVisits().set(i, oldVisits.get(j));
          }
        }
      });
      ret.add(StatResp.builder()
        .city("TOTAL")
        .times(times)
        .visits(visits)
        .build());
    }
    return responseOf(ret);
  }

  @Data
  @Builder
  private static class StatResp {
    private String city;
    private List<String> times;
    private List<Integer> visits;
  }
}
