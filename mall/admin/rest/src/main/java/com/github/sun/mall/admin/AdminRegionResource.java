package com.github.sun.mall.admin;

import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.mall.admin.entity.Admin;
import com.github.sun.mall.core.RegionMapper;
import com.github.sun.mall.core.entity.Region;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Builder;
import lombok.Data;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/v1/mall/admin/region")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-admin: 区域管理: region")
public class AdminRegionResource extends AbstractResource {
  private final RegionMapper mapper;

  @Inject
  public AdminRegionResource(RegionMapper mapper) {
    this.mapper = mapper;
  }

  @GET
  @ApiOperation("获取区域树")
  public ListResponse<Tree> getAll(@Context Admin admin) {
    List<Region> list = mapper.findAll();
    Map<String, List<Region>> map = list.stream().collect(Collectors.groupingBy(Region::getPId));
    List<Tree> provinces = list.stream().filter(v -> v.getType() == 1)
      .map(v -> Tree.builder()
        .id(v.getId())
        .name(v.getName())
        .code(v.getCode())
        .type(v.getType())
        .build())
      .collect(Collectors.toList());
    return responseOf(makeTree(map, provinces));
  }

  private List<Tree> makeTree(Map<String, List<Region>> map, List<Tree> roots) {
    class Util {
      private Tree makeTree(Tree node) {
        List<Tree> arr = map.getOrDefault(node.getId(), Collections.emptyList()).stream()
          .map(v -> Tree.builder()
            .id(v.getId())
            .name(v.getName())
            .code(v.getCode())
            .type(v.getType())
            .build())
          .collect(Collectors.toList());
        List<Tree> children = arr.stream().map(this::makeTree).collect(Collectors.toList());
        node.setChildren(children);
        return node;
      }
    }
    Util u = new Util();
    return roots.stream().map(u::makeTree).collect(Collectors.toList());
  }

  @Data
  @Builder
  private static class Tree {
    private String id;
    private String name;
    private int code;
    private int type;
    private List<Tree> children;
  }

  @GET
  @Path("/${id}")
  @ApiOperation("获取子区域列表")
  public ListResponse<Region> children(@PathParam("id") String id,
                                       @Context Admin admin) {
    return responseOf(mapper.findByPId(id));
  }
}
