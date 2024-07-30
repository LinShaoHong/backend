package com.github.sun.word;

import com.github.sun.foundation.boot.utility.Dates;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;

@Service
@RefreshScope
public class WordCodeService {
  @Resource
  private WordCodeMapper mapper;
  @Resource
  private WordCheckerMapper checkerMapper;

  @Transactional
  public synchronized int genWordSort() {
    String date = Dates.format(new Date());
    long code = 1;
    String id = date + ":" + code;
    WordCode entity = mapper.queryForUpdate(id);
    if (entity != null) {
      code = entity.getCode() + 1;
      mapper.updateById(entity.getId(), code);
    } else {
      WordCode v = new WordCode();
      v.setId(id);
      v.setType(date);
      v.setCode(code);
      mapper.insert(v);

      WordChecker checker = new WordChecker();
      checker.setId(date);
      checker.setSort(1);
      checker.setViewed(0);
      checker.setTotal(0);
      checkerMapper.insert(checker);
    }
    return ((Long) code).intValue();
  }
}