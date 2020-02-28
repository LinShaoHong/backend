package com.github.sun.mall.core.resolver;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginUser {
  private String id;
  private String name;
}
