package com.github.sun.qm;

import com.github.sun.foundation.sql.SqlBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Service
public class FootprintService {
  @Resource
  private FootprintMapper mapper;
  @Resource(name = "mysql")
  private SqlBuilder.Factory factory;

  @Value("${footprint.max}")
  private int max;

  @Transactional
  public void record(String userId, String girlId) {
    SqlBuilder sb = factory.create();
    Footprint exist = mapper.findById(Footprint.makeId(userId, girlId));
    if (exist != null) {
      mapper.updateTime(exist.getId(), new Date());
    } else {
      int total = mapper.countByTemplate(sb.from(Footprint.class).where(sb.field("userId").eq(userId)).count().template());
      if (total >= max) {
        sb.clear();
        SqlBuilder.Template template = sb.from(Footprint.class)
          .where(sb.field("userId").eq(userId))
          .desc("updateTime")
          .limit(max - 1, total)
          .template();
        List<Footprint> deletes = mapper.findByTemplate(template);
        mapper.deleteAll(deletes);
      }
      Footprint footprint = Footprint.builder()
        .id(Footprint.makeId(userId, girlId))
        .userId(userId)
        .girlId(girlId)
        .build();
      mapper.insertOrUpdate(footprint);
    }
  }
}
