package com.github.sun.qm;

import com.github.sun.foundation.boot.exception.Message;
import com.github.sun.foundation.sql.IdGenerator;
import com.github.sun.foundation.sql.SqlBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class ChargeService {
  @Resource
  private ChargeMapper mapper;
  @Resource
  private ChargeMapper.YQMapper yqMapper;
  @Resource
  private UserMapper userMapper;
  @Resource
  private GirlMapper girlMapper;
  @Resource
  private PayLogMapper payLogMapper;
  @Resource(name = "mysql")
  private SqlBuilder.Factory factory;

  @Transactional
  public void recharge(String id, User user) {
    Charge charge = mapper.findById(id);
    if (charge == null) {
      throw new Message(3000);
    }
    if (charge.isUsed()) {
      throw new Message(3001);
    }
    charge.setUsed(true);
    charge.setUserId(user.getId());
    mapper.update(charge);

    user.setVip(charge.isVip());
    List<Charge.YQ> yqs = yqMapper.findAll();
    yqs.stream().filter(v -> v.getType() == charge.getType()).findFirst().ifPresent(yq -> {
      final BigDecimal price = new BigDecimal(yq.getAmount());
      if (charge.isVip()) {
        Date now = new Date();
        user.setVipStartTime(now);
        Calendar c = Calendar.getInstance();
        c.setTime(now);
        switch (charge.getType()) {
          case VIP_MONTH:
            c.add(Calendar.MONTH, 1);
            break;
          case VIP_QUARTER:
            c.add(Calendar.MONTH, 3);
            break;
          case VIP_YEAR:
            c.add(Calendar.YEAR, 1);
            break;
          case VIP_FOREVER:
            c.add(Calendar.YEAR, 100);
            break;
        }
        user.setVipEndTime(c.getTime());
      } else {
        user.setAmount(user.getAmount() == null ? price : price.add(user.getAmount()));
      }
      userMapper.update(user);
      payLogMapper.insert(PayLog.builder()
        .id(IdGenerator.next())
        .userId(user.getId())
        .type(PayLog.Type.RECHARGE)
        .amount(price)
        .chargeType(charge.getType().name())
        .build());

      if (user.isVip()) {
        SqlBuilder sb = factory.create();
        SqlBuilder.Template template = sb.from(Girl.class)
          .update()
          .set("payments", sb.field("payments").plus(1))
          .template();
        girlMapper.updateByTemplate(template);
      }
    });
  }

  @Transactional
  public void consume(String girlId, User user) {
    if (!user.isVip()) {
      Girl girl = girlMapper.findById(girlId);
      if (girl == null || !girl.isOnService()) {
        throw new Message(2000);
      }
      BigDecimal amount = user.getAmount() == null ? new BigDecimal(0) : user.getAmount();
      BigDecimal price = girl.getPrice() == null ? new BigDecimal(0) : girl.getPrice();
      if (price.compareTo(amount) > 0) {
        throw new Message(2001);
      } else if (price.compareTo(new BigDecimal(0)) > 0) {
        amount = amount.subtract(price);
        user.setAmount(amount);
        userMapper.update(user);
        payLogMapper.insert(PayLog.builder()
          .id(IdGenerator.next())
          .userId(user.getId())
          .amount(girl.getPrice())
          .type(PayLog.Type.PAYMENT)
          .girlId(girlId)
          .build());

        SqlBuilder sb = factory.create();
        SqlBuilder.Template template = sb.from(Girl.class)
          .where(sb.field("id").eq(girlId))
          .update()
          .set("payments", sb.field("payments").plus(1))
          .template();
        girlMapper.updateByTemplate(template);
      }
    }
  }
}
