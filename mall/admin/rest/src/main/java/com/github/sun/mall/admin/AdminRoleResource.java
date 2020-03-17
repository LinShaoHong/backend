package com.github.sun.mall.admin;

import com.github.sun.foundation.boot.exception.BadRequestException;
import com.github.sun.foundation.boot.exception.NotFoundException;
import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.IdGenerator;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.mall.admin.auth.Authentication;
import com.github.sun.mall.admin.entity.Admin;
import com.github.sun.mall.admin.entity.Role;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.*;

@Path("/v1/mall/admin/role")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-admin: 角色管理: role")
public class AdminRoleResource extends AbstractResource {
  private final RoleMapper mapper;
  private final AdminMapper adminMapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public AdminRoleResource(RoleMapper mapper,
                           AdminMapper adminMapper,
                           @Named("mysql") SqlBuilder.Factory factory) {
    this.mapper = mapper;
    this.adminMapper = adminMapper;
    this.factory = factory;
  }

  @GET
  @ApiOperation("分页查询角色")
  @Authentication(value = "admin:role:query", tags = {"系统管理", "角色管理", "查询"})
  public PageResponse<Role> list(@QueryParam("name") String name,
                                 @QueryParam("start") int start,
                                 @QueryParam("count") int count,
                                 @QueryParam("sort") @DefaultValue("createTime") String sort,
                                 @QueryParam("asc") boolean asc,
                                 @Context Admin admin) {
    SqlBuilder sb = factory.create();
    Expression condition = Expression.nonNull(name).then(sb.field("name").contains(name));
    int total = mapper.countByTemplate(sb.from(Role.class).where(condition).count().template());
    if (start < total) {
      sb.clear();
      SqlBuilder.Template template = sb.from(Role.class)
        .where(condition)
        .orderBy(sort, asc)
        .limit(start, count)
        .template();
      final List<Role> list = mapper.findByTemplate(template);
      return responseOf(total, list);
    }
    return responseOf(total, Collections.emptyList());
  }

  @GET
  @Path("/options")
  @ApiOperation("获取所有角色")
  public ListResponse<Map<String, Object>> options(@Context Admin admin) {
    List<Role> roles = mapper.findAll();
    List<Map<String, Object>> options = new ArrayList<>(roles.size());
    for (Role role : roles) {
      Map<String, Object> option = new HashMap<>(2);
      option.put("value", role.getId());
      option.put("label", role.getName());
      options.add(option);
    }
    return responseOf(options);
  }

  @GET
  @Path("/${id}")
  @ApiOperation("角色详情")
  @Authentication(value = "admin:role:detail", tags = {"系统管理", "角色管理", "详情"})
  public SingleResponse<Role> get(@PathParam("id") String id,
                                  @Context Admin admin) {
    Role role = mapper.findById(id);
    if (role == null) {
      throw new NotFoundException("Can not find role by id" + id);
    }
    return responseOf(role);
  }

  @POST
  @ApiOperation("创建角色")
  @Authentication(value = "admin:role:create", tags = {"系统管理", "角色管理", "添加"})
  public Response create(@Valid @NotNull(message = "缺少实体") Role role,
                         @Context Admin admin) {
    if (mapper.findByName(role.getName()) != null) {
      throw new BadRequestException("角色已经存在");
    }
    role.setId(IdGenerator.next());
    mapper.insert(role);
    return responseOf();
  }

  @PUT
  @Path("/${id}")
  @ApiOperation("编辑角色")
  @Authentication(value = "admin:role:update", tags = {"系统管理", "角色管理", "编辑"})
  public Response update(@PathParam("id") String id,
                         @Valid @NotNull(message = "缺少实体") Role role,
                         @Context Admin admin) {
    Role r = mapper.findById(id);
    if (r == null) {
      throw new NotFoundException("Can not find role by id" + id);
    }
    role.setId(id);
    mapper.update(role);
    return responseOf();
  }

  @DELETE
  @Path("/${id}")
  @ApiOperation("删除角色")
  @Authentication(value = "admin:role:delete", tags = {"系统管理", "角色管理", "删除"})
  public Response delete(@PathParam("id") String id) {
    Role r = mapper.findById(id);
    if (r == null) {
      throw new NotFoundException("Can not find role by id" + id);
    }
    // 如果当前角色所对应管理员仍存在，则拒绝删除角色
    if (adminMapper.countByRoleIdsContains(id) > 0) {
      throw new BadRequestException("当前角色存在管理员，不能删除");
    }
    mapper.deleteById(id);
    return responseOf();
  }
}
