package com.github.sun.mall.core;

import com.github.sun.foundation.boot.exception.NotFoundException;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.mall.core.entity.Address;
import com.github.sun.mall.core.entity.User;
import com.github.sun.mall.core.resolver.LoginUser;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

@Path("/v1/mall/address")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-core: 收货地址服务: address")
public class AddressResource extends AbstractResource {
  private final AddressMapper mapper;
  private final AddressService service;

  @Inject
  public AddressResource(AddressMapper mapper, AddressService service) {
    this.mapper = mapper;
    this.service = service;
  }

  @GET
  @ApiOperation("获取收货地址列表")
  public ListResponse<Address> getAll(@Context LoginUser user) {
    return responseOf(mapper.findByUserId(user.getId()));
  }

  @GET
  @Path("/${id}")
  @ApiOperation("获取收货地址")
  public SingleResponse<Address> get(@PathParam("id") String id, @Context LoginUser user) {
    Address address = mapper.findByIdAndUserId(id, user.getId());
    if (address == null) {
      throw new NotFoundException("Can not find address by id=" + id);
    }
    return responseOf(address);
  }

  @POST
  @ApiOperation("增加或更新收货地址")
  public Object save(@Valid @NotNull(message = "缺少实体") Address address, @Context LoginUser user) {
    address.setUserId(user.getId());
    service.insertOrUpdate(address);
    return responseOf();
  }

  @DELETE
  @Path("/${id}")
  @ApiOperation("删除收货地址")
  public Response delete(@PathParam("id") String id, @Context LoginUser user) {
    Address address = mapper.findById(id);
    if (address == null) {
      throw new NotFoundException("Can not find address by id=" + id);
    }
    mapper.deleteById(id);
    return responseOf();
  }
}
