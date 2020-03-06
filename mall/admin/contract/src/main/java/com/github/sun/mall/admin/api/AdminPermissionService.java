package com.github.sun.mall.admin.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public interface AdminPermissionService {
  List<Node> getSystemPermTree();

  void update(String roleId, Set<String> permissions);

  @Data
  @Builder
  class Node {
    private String id;
    @JsonIgnore
    private String parentId;
    private String label;
    private String api;
    private List<Node> children;

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Node node = (Node) o;
      return id.equals(node.id);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id);
    }
  }
}
