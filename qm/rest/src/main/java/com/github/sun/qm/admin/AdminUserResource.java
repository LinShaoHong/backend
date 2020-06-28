package com.github.sun.qm.admin;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.qm.User;
import com.github.sun.qm.UserMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Builder;
import lombok.Data;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.util.*;

@Path("/v1/qm/admin/user")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Admin User Resource")
public class AdminUserResource extends AbstractResource {
  private final UserMapper mapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public AdminUserResource(UserMapper mapper, @Named("mysql") SqlBuilder.Factory factory) {
    this.mapper = mapper;
    this.factory = factory;
  }

  @GET
  @ApiOperation("分页获取用户信息")
  public PageResponse<ObjectNode> paged(@QueryParam("id") String id,
                                        @QueryParam("username") String username,
                                        @QueryParam("email") String email,
                                        @QueryParam("vip") Boolean vip,
                                        @QueryParam("start") int start,
                                        @QueryParam("count") int count,
                                        @DefaultValue("createTime") @QueryParam("rank") String rank,
                                        @Context Admin admin) {
    SqlBuilder sb = factory.create();
    Expression condition = Expression.nonEmpty(id).then(sb.field("id").eq(id))
      .and(username == null || username.isEmpty() ? null : sb.field("username").eq(username.trim()))
      .and(email == null || email.isEmpty() ? null : sb.field("email").eq(email))
      .and(vip == null ? null : sb.field("vip").eq(vip));
    int total = mapper.countByTemplate(sb.from(User.class).where(condition).count().template());
    if (start < total) {
      sb.clear();
      SqlBuilder.Template template = sb.from(User.class)
        .where(condition)
        .desc(rank)
        .limit(start, count)
        .template();
      List<User> list = mapper.findByTemplate(template);
      return responseOf(total, JSON.deserializeAsList(list, ObjectNode.class));
    }
    return responseOf(total, Collections.emptyList());
  }

  @PUT
  @Path("/${id}")
  @ApiOperation("更改用户")
  public Response update(@PathParam("id") String id,
                         UserReq req,
                         @Context Admin admin) {
    User u = mapper.findById(id);
    if (u != null) {
      u.setAmount(req.getAmount());
      mapper.update(u);
    }
    return responseOf();
  }

  @Data
  private static class UserReq {
    private BigDecimal amount;
  }

  @GET
  @Path("/stat")
  @ApiOperation("统计用户")
  public SingleResponse<StatResp> statUser(@QueryParam("timeType") int timeType,
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
    SqlBuilder.Template template = sb.from(User.class)
      .select(sb.field("id").count())
      .template();
    int total = mapper.countByTemplate(template);

    template = sb.from(User.class)
      .where(sb.id("UNIX_TIMESTAMP").call(sb.field("createTime")).ge(c.getTimeInMillis() / 1000))
      .groupBy(sb.field("substr").call(sb.field("createTime"), 1, 10))
      .select(sb.field("substr").call(sb.field("createTime"), 1, 10), "time")
      .select(sb.field("id").distinct().count(), "count")
      .template();
    List<Map<String, Object>> list = mapper.findByTemplateAsMap(template);

    List<String> times = new ArrayList<>();
    List<Integer> nums = new ArrayList<>();
    List<Integer> totals = new ArrayList<>();

    Collections.reverse(list);
    for (Map<String, Object> map : list) {
      times.add((String) map.get("time"));
      Integer inc = ((Long) map.get("count")).intValue();
      nums.add(inc);


      totals.add(total);
      total -= inc;
    }

    Collections.reverse(times);
    Collections.reverse(nums);
    Collections.reverse(totals);
    return responseOf(StatResp.builder()
      .times(times)
      .nums(nums)
      .totals(totals)
      .build());
  }

  @Data
  @Builder
  private static class StatResp {
    private List<String> times;
    private List<Integer> nums;
    private List<Integer> totals;
  }
}
