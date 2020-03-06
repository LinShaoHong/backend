package com.github.sun.mall.admin;

import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.mall.admin.api.AdminConfigService;
import com.github.sun.mall.admin.auth.Authentication;
import com.github.sun.mall.admin.entity.Admin;
import com.github.sun.mall.admin.entity.System;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/v1/mall/admin/config")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-admin: 配置管理: config")
public class AdminConfigResource extends AbstractResource {
  private final SystemMapper mapper;
  private final AdminConfigService service;

  @Inject
  public AdminConfigResource(SystemMapper mapper, AdminConfigService service) {
    this.mapper = mapper;
    this.service = service;
  }

  @GET
  @Path("/mall")
  @ApiOperation("获取商场配置")
  @Authentication(value = "admin:config:mall:query", tags = {"配置管理", "商场配置", "详情"})
  public SingleResponse<Map<String, String>> listMall(@Context Admin admin) {
    return responseOf(findAll("mall_mall"));
  }

  @PUT
  @Path("/mall")
  @ApiOperation("更改商场配置")
  @Authentication(value = "admin:config:mall:update", tags = {"配置管理", "商场配置", "编辑"})
  public Response updateMall(@Valid @NotNull(message = "缺少实体") Map<String, String> data, @Context Admin admin) {
    service.update(data);
    return responseOf();
  }

  @GET
  @Path("/express")
  @ApiOperation("获取运费配置")
  @Authentication(value = "admin:config:express:query", tags = {"配置管理", "运费配置", "详情"})
  public SingleResponse<Map<String, String>> listExpress(@Context Admin admin) {
    return responseOf(findAll("mall_express"));
  }

  @PUT
  @Path("/express")
  @ApiOperation("更改运费配置")
  @Authentication(value = "admin:config:express:update", tags = {"配置管理", "运费配置", "编辑"})
  public Response updateExpress(@Valid @NotNull(message = "缺少实体") Map<String, String> data, @Context Admin admin) {
    service.update(data);
    return responseOf();
  }

  @GET
  @Path("/order")
  @ApiOperation("获取订单配置")
  @Authentication(value = "admin:config:order:query", tags = {"配置管理", "订单配置", "详情"})
  public SingleResponse<Map<String, String>> lisOrder(@Context Admin admin) {
    return responseOf(findAll("mall_order"));
  }

  @PUT
  @Path("/order")
  @ApiOperation("更改订单配置")
  @Authentication(value = "admin:config:order:update", tags = {"配置管理", "订单配置", "编辑"})
  public Response updateOrder(@Valid @NotNull(message = "缺少实体") Map<String, String> data, @Context Admin admin) {
    service.update(data);
    return responseOf();
  }

  @GET
  @Path("/wx")
  @ApiOperation("获取小程序配置")
  @Authentication(value = "admin:config:wx:query", tags = {"配置管理", "小程序配置", "详情"})
  public SingleResponse<Map<String, String>> listWx(@Context Admin admin) {
    return responseOf(findAll("mall_wx"));
  }

  @PUT
  @Path("/wx")
  @ApiOperation("更改小程序配置")
  @Authentication(value = "admin:config:wx:update", tags = {"配置管理", "小程序配置", "编辑"})
  public Response updateWx(@Valid @NotNull(message = "缺少实体") Map<String, String> data, @Context Admin admin) {
    service.update(data);
    return responseOf();
  }

  private Map<String, String> findAll(String name) {
    List<System> list = mapper.findByKeyNameStartsWith(name);
    return list.stream().collect(Collectors.toMap(System::getKeyName, System::getKeyValue, (m1, m2) -> m1));
  }
}
