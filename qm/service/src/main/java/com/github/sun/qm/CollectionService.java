package com.github.sun.qm;

import com.github.sun.foundation.sql.IdGenerator;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class CollectionService {
  @Resource
  private CollectionMapper mapper;
  @Resource
  private GirlMapper girlMapper;

  public void add(String userId, String girlId) {
    if (mapper.countByUserIdAndGirlId(userId, girlId) == 0) {
      Girl girl = girlMapper.findById(girlId);
      if (girl != null && girl.isOnService()) {
        Collection collection = Collection.builder()
          .id(IdGenerator.next())
          .userId(userId)
          .girlId(girlId)
          .build();
        mapper.insert(collection);
        girl.setCollects(girl.getCollects() + 1);
        girlMapper.update(girl);
      }
    }
  }
}
