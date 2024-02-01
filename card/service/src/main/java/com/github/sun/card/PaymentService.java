package com.github.sun.card;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {
  public PayResp wxPay(String userId, String amount) {
    return PayResp.builder()
      .timeStamp("111")
      .nonceStr("111")
      .packages("111")
      .paySign("111")
      .signType("111")
      .build();
  }

  @Data
  @Builder
  public static class PayResp {
    private String timeStamp;
    private String nonceStr;
    private String packages;
    private String paySign;
    private String signType;
  }
}