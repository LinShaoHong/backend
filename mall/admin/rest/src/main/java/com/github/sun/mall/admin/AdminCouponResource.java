package com.github.sun.mall.admin;

import com.github.sun.foundation.boot.exception.NotFoundException;
import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.IdGenerator;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.mall.admin.auth.Authentication;
import com.github.sun.mall.admin.entity.Admin;
import com.github.sun.mall.core.CouponMapper;
import com.github.sun.mall.core.entity.Coupon;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;

@Path("/v1/mall/admin/coupon")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-admin: 优惠券管理: coupon")
public class AdminCouponResource extends AbstractResource {
  private final CouponMapper mapper;
  private final CouponMapper.User couponUserMapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public AdminCouponResource(CouponMapper mapper,
                             CouponMapper.User couponUserMapper,
                             @Named("mysql") SqlBuilder.Factory factory) {
    this.mapper = mapper;
    this.couponUserMapper = couponUserMapper;
    this.factory = factory;
  }

  @GET
  @ApiOperation("分页获取优惠券")
  @Authentication(value = "admin:coupon:query", tags = {"推广管理", "优惠券管理", "查询"})
  public PageResponse<Coupon> list(@QueryParam("name") String name,
                                   @Pattern(regexp = "COMMON|REGISTER|CODE", message = "type非法") @QueryParam("type") String type,
                                   @Pattern(regexp = "NORMAL|EXPIRED|OUT", message = "status非法") @QueryParam("status") String status,
                                   @QueryParam("start") int start,
                                   @QueryParam("count") int count,
                                   @QueryParam("sort") @DefaultValue("createTime") String sort,
                                   @QueryParam("asc") boolean asc,
                                   @Context Admin admin) {
    SqlBuilder sb = factory.create();
    Expression condition = Expression.nonNull(name).then(sb.field("name").contains(name))
      .and(type == null ? null : sb.field("type").eq(type))
      .and(status == null ? null : sb.field("status").eq(status));
    int total = mapper.countByTemplate(sb.from(Coupon.class).where(condition).count().template());
    if (start < total) {
      sb.clear();
      SqlBuilder.Template template = sb.from(Coupon.class)
        .where(condition)
        .orderBy(sort, asc)
        .limit(start, count)
        .template();
      final List<Coupon> list = mapper.findByTemplate(template);
      return responseOf(total, list);
    }
    return responseOf(total, Collections.emptyList());
  }

  @GET
  @Path("")
  @Authentication(value = "admin:coupon:queryUser", tags = {"推广管理", "优惠券管理", "查询用户"})
  public PageResponse<Coupon.User> queryUser(@QueryParam("userId") String userId,
                                             @QueryParam("couponId") String couponId,
                                             @Pattern(regexp = "NORMAL|EXPIRED|OUT", message = "status非法") @QueryParam("status") String status,
                                             @QueryParam("start") int start,
                                             @QueryParam("count") int count,
                                             @QueryParam("sort") @DefaultValue("createTime") String sort,
                                             @QueryParam("asc") boolean asc,
                                             @Context Admin admin) {

    SqlBuilder sb = factory.create();
    Expression condition = Expression.nonNull(userId).then(sb.field("userId").eq(userId))
      .and(couponId == null ? null : sb.field("couponId").eq(couponId));
    int total = couponUserMapper.countByTemplate(sb.from(Coupon.User.class).where(condition).count().template());
    if (start < total) {
      SqlBuilder.Template template = sb.from(Coupon.User.class)
        .where(condition)
        .orderBy(sort, asc)
        .limit(start, count)
        .template();
      final List<Coupon.User> list = couponUserMapper.findByTemplate(template);
      return responseOf(total, list);
    }
    return responseOf(total, Collections.emptyList());
  }

  @POST
  @Authentication(value = "admin:coupon:create", tags = {"推广管理", "优惠券管理", "添加"})
  public Response create(@Valid @NotNull(message = "缺少实体") Coupon coupon,
                         @Context Admin admin) {
    // 如果是兑换码类型，则这里需要生存一个兑换码
    if (coupon.getType().equals(Coupon.Type.CODE)) {
      // todo
      String code = "couponService.generateCode();" + IdGenerator.next();
      coupon.setCode(code);
    }
    coupon.setId(IdGenerator.next());
    mapper.insert(coupon);
    return responseOf();
  }

  @GET
  @Path("/${id}")
  @ApiOperation("获取详情")
  @Authentication(value = "admin:coupon:detail", tags = {"推广管理", "优惠券管理", "详情"})
  public SingleResponse<Coupon> read(@PathParam("id") String id,
                                     @Context Admin admin) {
    Coupon coupon = mapper.findById(id);
    if (coupon == null) {
      throw new NotFoundException("Can not find Coupon by id=" + id);
    }
    return responseOf(coupon);
  }

  @PUT
  @Path("/${id}")
  @ApiOperation("编辑")
  @Authentication(value = "admin:coupon:update", tags = {"推广管理", "优惠券管理", "编辑"})
  public Response update(@PathParam("id") String id,
                         @Valid @NotNull(message = "缺少实体") Coupon coupon,
                         @Context Admin admin) {

    Coupon e = mapper.findById(id);
    if (e == null) {
      throw new NotFoundException("Can not find Coupon by id=" + id);
    }
    coupon.setId(id);
    mapper.update(coupon);
    return responseOf();
  }

  @DELETE
  @Path("/${id}")
  @ApiOperation("删除")
  @Authentication(value = "admin:coupon:delete", tags = {"推广管理", "优惠券管理", "删除"})
  public Response delete(@PathParam("id") String id,
                         @Context Admin admin) {
    Coupon e = mapper.findById(id);
    if (e == null) {
      throw new NotFoundException("Can not find Coupon by id=" + id);
    }
    mapper.deleteById(id);
    return responseOf();
  }
}
