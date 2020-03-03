package com.github.sun.mall.admin.auth;

import java.lang.annotation.*;

@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Authentication {
  String value();

  String[] tags();
}
