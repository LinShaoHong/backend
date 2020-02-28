package com.github.sun.mall.admin;

import com.github.sun.foundation.boot.exception.NotFoundException;
import com.github.sun.foundation.mybatis.CompositeMapper;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.IdGenerator;
import com.github.sun.mall.core.entity.Entity;
import io.swagger.annotations.ApiOperation;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;

public abstract class BasicCURDResource<V extends Entity<String>, M extends CompositeMapper<V>> extends AbstractResource {
  @Resource
  protected M mapper;

  protected abstract String name();

  @POST
  @ApiOperation("创建")
  public Response create(@Valid @NotNull(message = "缺少实体") V v) {
    v.setId(IdGenerator.next());
    mapper.insert(v);
    return responseOf();
  }

  @GET
  @Path("/${id}")
  @ApiOperation("获取")
  public SingleResponse<V> get(@PathParam("id") String id) {
    V v = mapper.findById(id);
    if (v == null) {
      throw new NotFoundException("Can not find " + name() + " by id=" + id);
    }
    return responseOf(v);
  }

  @PUT
  @Path("/${id}")
  @ApiOperation("编辑")
  public Response update(@PathParam("id") String id,
                         @Valid @NotNull(message = "缺少实体") V v) {
    V e = mapper.findById(id);
    if (e == null) {
      throw new NotFoundException("Can not find ad " + name() + " id=" + id);
    }
    v.setId(id);
    mapper.update(v);
    return responseOf();
  }

  @DELETE
  @Path("/${id}")
  @ApiOperation("删除")
  public Response delete(@PathParam("id") String id) {
    V v = mapper.findById(id);
    if (v == null) {
      throw new NotFoundException("Can not find ad " + name() + " id=" + id);
    }
    mapper.deleteById(id);
    return responseOf();
  }
}
