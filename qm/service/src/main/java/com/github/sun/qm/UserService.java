package com.github.sun.qm;

import com.github.sun.foundation.sql.IdGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

@Service
public class UserService {
  private static final SimpleDateFormat DAY_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");

  @Resource
  private UserMapper mapper;
  @Resource
  private PayLogMapper payLogMapper;

  @Transactional
  public User signIn(User user) {
    String signTime = user.getSignInTime() == null ? null : DAY_FORMATTER.format(user.getSignInTime());
    String today = DAY_FORMATTER.format(new Date());
    if (!Objects.equals(signTime, today)) {
      user.setSignInTime(new Date());
      user.setAmount(user.getAmount() == null ? new BigDecimal(1) : new BigDecimal(1).add(user.getAmount()));
      user.setSignInCount(user.getSignInCount() + 1);
      mapper.update(user);
      PayLog log = PayLog.builder()
        .id(IdGenerator.next())
        .userId(user.getId())
        .amount(new BigDecimal(1))
        .type(PayLog.Type.SING_IN)
        .build();
      payLogMapper.insert(log);
    }
    return user;
  }
}
