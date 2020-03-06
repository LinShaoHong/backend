package com.github.sun.mall.admin;

import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.mall.admin.api.AdminPermissionService;
import com.github.sun.mall.admin.auth.Authentication;
import com.github.sun.mall.admin.entity.Admin;
import com.github.sun.mall.admin.entity.Permission;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Builder;
import lombok.Data;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Path("/v1/mall/admin/permission")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-admin: 权限管理: permission")
public class AdminPermissionResource extends AbstractResource {
  private final PermissionMapper mapper;
  private final AdminPermissionService service;

  @Inject
  public AdminPermissionResource(PermissionMapper mapper,
                                 AdminPermissionService service) {
    this.mapper = mapper;
    this.service = service;
  }

  @GET
  @ApiOperation("获取权限树")
  @Authentication(value = "admin:role:permission:query", tags = {"系统管理", "角色管理", "权限详情"})
  public SingleResponse<PermissionResp> getPermissions(@NotEmpty(message = "require roleId")
                                                       @QueryParam("roleId") String roleId,
                                                       @Context Admin admin) {
    List<AdminPermissionServiceImpl.Node> system = service.getSystemPermTree();
    Set<String> assigned;
    List<Permission> ps = mapper.findByRoleId(roleId);
    if (ps.stream().anyMatch(p -> p.getPermission().equalsIgnoreCase("*"))) {
      class Util {
        private void traverse(AdminPermissionServiceImpl.Node node, Consumer<AdminPermissionServiceImpl.Node> func) {
          func.accept(node);
          node.getChildren().forEach(v -> traverse(v, func));
        }
      }
      Util u = new Util();
      assigned = new HashSet<>();
      for (AdminPermissionServiceImpl.Node n : system) {
        u.traverse(n, node -> {
          if (node.getApi() != null) {
            assigned.add(node.getId());
          }
        });
      }
    } else {
      assigned = ps.stream().map(Permission::getPermission).collect(Collectors.toSet());
    }
    return responseOf(PermissionResp.builder().system(system).assigned(assigned).build());
  }

  @Data
  @Builder
  private static class PermissionResp {
    private List<AdminPermissionServiceImpl.Node> system;
    private Set<String> assigned;
  }

  @PUT
  @ApiOperation("更新权限")
  @Authentication(value = "admin:role:permission:update", tags = {"系统管理", "角色管理", "权限变更"})
  public Response updatePermissions(@Valid @NotNull(message = "缺少实体") PermissionReq req,
                                    @Context Admin admin) {
    service.update(req.getRoleId(), req.getPermissions());
    return responseOf();
  }

  @Data
  @Builder
  private static class PermissionReq {
    @NotEmpty(message = "缺少roleId")
    private String roleId;
    @NotNull(message = "缺少permission")
    @Size(min = 1, message = "缺少permission")
    private Set<String> permissions;
  }
}
