package com.github.sun.mall.core.resolver;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MayLoginUser {
  private String id;
  private String name;

  public boolean login() {
    return id != null;
  }
}
