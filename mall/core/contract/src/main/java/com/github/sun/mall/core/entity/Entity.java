package com.github.sun.mall.core.entity;

import java.io.Serializable;

public interface Entity<K extends Serializable> {
  K getId();

  void setId(K id);
}
