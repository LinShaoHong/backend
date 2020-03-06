package com.github.sun.mall.admin;

import com.github.sun.mall.admin.api.AdminConfigService;
import com.github.sun.mall.admin.entity.System;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AdminConfigServiceImpl implements AdminConfigService {
  @Resource
  private SystemMapper mapper;

  @Override
  @Transactional
  public void update(Map<String, String> configs) {
    if (!configs.isEmpty()) {
      Set<String> keyNames = configs.keySet();
      List<System> exists = mapper.findByKeyNameIn(keyNames);
      List<System> updates = new ArrayList<>();
      List<System> inserts = new ArrayList<>();
      configs.forEach((key, value) -> {
        System exist = exists.stream().filter(v -> v.getKeyName().equals(key)).findFirst().orElse(null);
        if (exist != null) {
          exist.setKeyValue(value);
          updates.add(exist);
        } else {
          inserts.add(System.builder()
            .id(key)
            .keyName(key)
            .keyValue(value)
            .build());
        }
      });
      if (!inserts.isEmpty()) {
        mapper.insertAll(inserts);
      }
      if (!updates.isEmpty()) {
        mapper.updateAll(updates);
      }
    }
  }
}
