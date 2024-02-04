package com.github.sun.card;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RefreshScope
@RequiredArgsConstructor
public class CardCodeService {
  private final CardCodeMapper mapper;

  @Transactional
  public String genCode(String type) {
    return genCode(type, 1000);
  }

  @Transactional
  public String genCode(String type, long start) {
    long code = start;
    String id = type + ":" + code;
    CardCode entity = mapper.queryForUpdate(id);
    if (entity != null) {
      code = entity.getCode() + 1;
      mapper.updateById(entity.getId(), code);
    } else {
      CardCode v = new CardCode();
      v.setId(id);
      v.setType(type);
      v.setCode(code);
      mapper.insert(v);
    }
    return String.valueOf(code);
  }
}