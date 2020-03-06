package com.github.sun.mall.admin;

import com.github.sun.foundation.boot.exception.AccessDeniedException;
import com.github.sun.foundation.boot.exception.BadRequestException;
import com.github.sun.foundation.boot.exception.NotFoundException;
import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.IdGenerator;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.mall.admin.auth.Authentication;
import com.github.sun.mall.admin.entity.Admin;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.util.StringUtils;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;


@Path("/v1/mall/admin/admin")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-admin: 管理员: admin")
public class AdminAdminResource extends AbstractResource {
  private final AdminMapper mapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public AdminAdminResource(AdminMapper mapper, @Named("mysql") SqlBuilder.Factory factory) {
    this.mapper = mapper;
    this.factory = factory;
  }

  @GET
  @ApiOperation("获取所有管理员列表")
  @Authentication(value = "admin:admin:query", tags = {"系统管理", "管理员管理", "查询"})
  public PageResponse<Admin> list(@QueryParam("username") String username,
                                  @QueryParam("start") int start,
                                  @QueryParam("count") int count,
                                  @QueryParam("sort") @DefaultValue("createTime") String sort,
                                  @QueryParam("asc") boolean asc,
                                  @Context Admin admin) {
    SqlBuilder sb = factory.create();
    Expression condition = Expression.nonNull(username).then(sb.field("username").contains(username));
    int total = mapper.countByTemplate(sb.from(Admin.class).where(condition).count().template());
    if (start < total) {
      SqlBuilder.Template template = sb.from(Admin.class)
        .where(condition)
        .orderBy(sort, asc)
        .limit(start, count)
        .template();
      final List<Admin> list = mapper.findByTemplate(template);
      return responseOf(total, list);
    }
    return responseOf(total, Collections.emptyList());
  }

  @POST
  @ApiOperation("创建一个管理员")
  @Authentication(value = "admin:admin:create", tags = {"系统管理", "管理员管理", "添加"})
  public Response create(@Valid @NotNull(message = "缺少实体") Admin req,
                         @Context Admin admin) {
    String password = req.getPassword();
    if (StringUtils.isEmpty(password) || password.length() < 6) {
      throw new BadRequestException("管理员密码长度不能小于6");
    }
    String username = req.getUsername();
    if (mapper.countByUsername(username) > 0) {
      throw new BadRequestException("管理员已经存在");
    }
    String hashPassword = Admin.hashPassword(req.getPassword());
    req.setPassword(hashPassword);
    req.setId(IdGenerator.next());
    mapper.insert(req);
    return responseOf();
  }

  @GET
  @Path("/${id}")
  @ApiOperation("获取管理员信息")
  @Authentication(value = "admin:admin:detail", tags = {"系统管理", "管理员管理", "详情"})
  public SingleResponse<Admin> get(@PathParam("id") String id,
                                   @Context Admin admin) {
    Admin exist = mapper.findById(id);
    if (exist == null) {
      throw new NotFoundException("Can not find admin by id=" + id);
    }
    return responseOf(exist);
  }

  @PUT
  @Path("/${id}")
  @ApiOperation("更新管理员信息")
  @Authentication(value = "admin:admin:update", tags = {"系统管理", "管理员管理", "编辑"})
  public Response update(@PathParam("id") String id,
                         @Valid @NotNull(message = "缺少实体") Admin req,
                         @Context Admin admin) {
    Admin exist = mapper.findById(id);
    if (exist == null) {
      throw new NotFoundException("Can not find admin by id=" + id);
    }
    // 不允许管理员通过编辑接口修改密码
    req.setPassword(exist.getPassword());
    req.setId(id);
    mapper.update(req);
    return responseOf();
  }

  @DELETE
  @Path("/${id}")
  @ApiOperation("删除管理员")
  @Authentication(value = "admin:admin:delete", tags = {"系统管理", "管理员管理", "删除"})
  public Response delete(@PathParam("id") String id,
                         @Context Admin admin) {
    // 管理员不能删除自身账号
    if (id.equals(admin.getId())) {
      throw new AccessDeniedException("管理员不能删除自己账号");
    }
    mapper.deleteById(id);
    return responseOf();
  }
}
