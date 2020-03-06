package com.github.sun.mall.admin;

import com.github.sun.foundation.boot.Scanner;
import com.github.sun.foundation.boot.exception.AccessDeniedException;
import com.github.sun.foundation.boot.utility.Iterators;
import com.github.sun.foundation.sql.IdGenerator;
import com.github.sun.mall.admin.api.AdminPermissionService;
import com.github.sun.mall.admin.auth.Authentication;
import com.github.sun.mall.admin.entity.Permission;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.ws.rs.*;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AdminPermissionServiceImpl implements AdminPermissionService {
  private static List<Node> cache = null;

  @Resource
  private PermissionMapper mapper;

  @Override
  public List<Node> getSystemPermTree() {
    if (cache == null) {
      String basePackage = getClass().getPackage().getName();
      List<PermResource> permResources = Scanner.getClassesWithAnnotation(Path.class)
        .stream()
        .filter(tag -> tag.runtimeClass().getPackage().getName().startsWith(basePackage))
        .map(tag -> {
          Class<?> resourceClass = tag.runtimeClass();
          String baseApi = resourceClass.getAnnotation(Path.class).value();
          List<PermMethod> methods = Stream.of(resourceClass.getDeclaredMethods())
            .map(method -> {
              Authentication authentication = method.getAnnotation(Authentication.class);
              if (authentication != null) {
                String requestMethod = null;
                for (Class<? extends Annotation> clz : Arrays.asList(GET.class, POST.class, PUT.class, DELETE.class)) {
                  Annotation a = method.getAnnotation(clz);
                  if (a != null) {
                    requestMethod = a.annotationType().getSimpleName();
                    break;
                  }
                }
                if (requestMethod != null) {
                  Path subPath = method.getAnnotation(Path.class);
                  String subApi = subPath == null ? "" : subPath.value();
                  return PermMethod.builder()
                    .api(baseApi + subApi)
                    .requestMethod(requestMethod)
                    .authentication(authentication)
                    .build();
                }
              }
              return null;
            }).filter(Objects::nonNull).collect(Collectors.toList());
          return PermResource.builder()
            .resourceClass(resourceClass)
            .methods(methods)
            .build();
        }).filter(v -> !v.getMethods().isEmpty()).collect(Collectors.toList());
      cache = parse(permResources);
    }
    return cache;
  }

  private List<Node> parse(List<PermResource> permResources) {
    Set<Node> nodes = permResources.stream().flatMap(p -> p.getMethods().stream().flatMap(m -> {
      String[] tags = m.authentication.tags();
      String permission = m.getAuthentication().value();
      List<Node> list = new ArrayList<>();
      for (int i = 0; i < tags.length; i++) {
        String id = i == tags.length - 1 ? permission : Iterators.slice(i + 1).stream().map(v -> tags[v]).collect(Collectors.joining(":"));
        String pId = i == 0 ? "" : Iterators.slice(i).stream().map(v -> tags[v]).collect(Collectors.joining(":"));
        list.add(Node.builder()
          .id(id)
          .parentId(pId)
          .label(tags[i])
          .api(i == tags.length - 1 ? m.getRequestMethod() + " " + m.getApi() : null)
          .build());
      }
      return list.stream();
    })).collect(Collectors.toSet());
    Map<String, List<Node>> map = nodes.stream().collect(Collectors.groupingBy(Node::getParentId));
    List<Node> roots = nodes.stream().filter(v -> v.getParentId().isEmpty()).collect(Collectors.toList());
    class Util {
      private Node makeTree(Node node) {
        List<Node> arr = map.getOrDefault(node.getId(), Collections.emptyList());
        List<Node> children = arr.stream().map(this::makeTree).collect(Collectors.toList());
        node.setChildren(children);
        return node;
      }
    }
    Util u = new Util();
    return roots.stream().map(u::makeTree).collect(Collectors.toList());
  }

  @Override
  @Transactional
  public void update(String roleId, Set<String> permissions) {
    Set<String> ps = new HashSet<>(permissions);
    List<Node> system = getSystemPermTree();
    class Util {
      private void traverse(AdminPermissionServiceImpl.Node node, Consumer<Node> func) {
        func.accept(node);
        node.getChildren().forEach(v -> traverse(v, func));
      }
    }
    Util u = new Util();
    system.forEach(n -> u.traverse(n, node -> {
      if (node.getApi() != null) {
        ps.remove(node.getId());
      }
    }));
    if (!ps.isEmpty()) {
      throw new BadRequestException("权限不存在:" + Iterators.mkString(ps, ", "));
    }
    boolean isSuperAdmin = !mapper.findByRoleIdAndPermission(roleId, "*").isEmpty();
    // 如果修改的角色是超级权限，则拒绝修改
    if (isSuperAdmin) {
      throw new AccessDeniedException("当前角色的超级权限不能变更");
    }
    // 先删除旧的权限，再更新新的权限
    mapper.deleteByRoleId(roleId);
    List<Permission> list = permissions.stream().map(p -> Permission.builder()
      .id(IdGenerator.next())
      .roleId(roleId)
      .permission(p)
      .build()).collect(Collectors.toList());
    if (!list.isEmpty()) {
      mapper.insertAll(list);
    }
  }

  @Data
  @Builder
  private static class PermResource {
    private Class<?> resourceClass;
    private List<PermMethod> methods;
  }

  @Data
  @Builder
  private static class PermMethod {
    private String api;
    private String requestMethod;
    private Authentication authentication;
  }
}
