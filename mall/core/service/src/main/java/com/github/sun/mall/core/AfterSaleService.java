package com.github.sun.mall.core;

import com.github.sun.foundation.boot.exception.AccessDeniedException;
import com.github.sun.foundation.boot.exception.BadRequestException;
import com.github.sun.foundation.boot.exception.NotFoundException;
import com.github.sun.foundation.mybatis.BasicService;
import com.github.sun.foundation.sql.IdGenerator;
import com.github.sun.mall.core.entity.AfterSale;
import com.github.sun.mall.core.entity.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Random;

@Service
public class AfterSaleService extends BasicService<String, AfterSale, AfterSaleMapper> {
  @Resource
  private OrderMapper orderMapper;

  @Transactional
  public void apply(String userId, AfterSale afterSale) {
    Order order = orderMapper.findById(afterSale.getOrderId());
    if (order == null) {
      throw new NotFoundException("Can not find order by id=" + afterSale.getOrderId());
    }
    AfterSale.Status status = order.getAfterSaleStatus();
    if (status == AfterSale.Status.ACCEPTED || status == AfterSale.Status.REFUND) {
      throw new BadRequestException("已申请售后");
    }
    // 订单必须完成才能进入售后流程。
    if (!order.isConfirm() && !order.isAutoConfirm()) {
      throw new BadRequestException("订单未完成，不能申请售后");
    }
    BigDecimal amount = order.getActualPrice().subtract(order.getFreightPrice());
    if (afterSale.getAmount().compareTo(amount) > 0) {
      throw new BadRequestException("退款金额不正确");
    }
    // 如果有旧的售后记录则删除（例如用户已取消，管理员拒绝）
    mapper.deleteByUserIdAndOrderId(userId, afterSale.getOrderId());
    afterSale.setId(IdGenerator.next());
    afterSale.setUserId(userId);
    afterSale.setStatus(AfterSale.Status.APPLIED);
    afterSale.setSn(generateAfterSaleSn(userId));
    mapper.insert(afterSale);
    orderMapper.updateAfterSaleStatus(afterSale.getId(), AfterSale.Status.APPLIED);
  }

  @Transactional
  public void cancel(String userId, String id) {
    AfterSale afterSale = mapper.findById(id);
    if (afterSale == null) {
      throw new NotFoundException("Can not find after-sale by id=" + id);
    }
    String orderId = afterSale.getOrderId();
    Order order = orderMapper.findById(orderId);
    if (Objects.equals(order.getUserId(), userId)) {
      throw new AccessDeniedException("非该订单的用户不能取消");
    }
    // 订单必须完成才能进入售后流程
    if (!order.isConfirm() && !order.isAutoConfirm()) {
      throw new BadRequestException("不支持售后");
    }
    AfterSale.Status afterStatus = order.getAfterSaleStatus();
    if (afterStatus != AfterSale.Status.APPLIED) {
      throw new BadRequestException("不能取消售后");
    }
    afterSale.setStatus(AfterSale.Status.CANCELED);
    mapper.update(afterSale);
    // 订单的afterSale_status和售后记录的status是一致的
    orderMapper.updateAfterSaleStatus(orderId, AfterSale.Status.CANCELED);
  }

  private String getRandomNum(int num) {
    Random random = new Random();
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < num; i++) {
      int number = random.nextInt("0123456789".length());
      sb.append("0123456789".charAt(number));
    }
    return sb.toString();
  }

  // todo 保证唯一
  private String generateAfterSaleSn(String userId) {
    DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyyMMdd");
    String now = df.format(LocalDate.now());
    String afterSaleSn = now + getRandomNum(6);
    while (mapper.countByUserIdAndAfterSaleSn(userId, afterSaleSn) != 0) {
      afterSaleSn = now + getRandomNum(6);
    }
    return afterSaleSn;
  }
}
