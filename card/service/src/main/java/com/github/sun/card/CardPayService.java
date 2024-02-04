package com.github.sun.card;

import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderRequest;
import com.github.binarywang.wxpay.bean.result.WxPayUnifiedOrderResult;
import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import com.github.binarywang.wxpay.service.impl.WxPayServiceImpl;
import com.github.binarywang.wxpay.util.SignUtils;
import com.github.sun.foundation.boot.exception.NotFoundException;
import com.github.sun.foundation.boot.utility.IPs;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.ws.rs.container.ContainerRequestContext;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CardPayService {
  @Value("${wx.appId}")
  private String wxAppId;
  @Value("${wx.pay.mchId}")
  private String wxMchId;
  @Value("${wx.pay.mchKey}")
  private String wxMchKey;
  @Value("${wx.pay.mchKey}")
  private String wxKeyPath;
  @Value("${wx.pay.notifyUrl}")
  private String wxNotifyUrl;
  private final CardUserMapper mapper;

  @Transactional
  public PayResp wxPay(String userId, String amount, int vip, ContainerRequestContext ctx) {
    CardUser user = mapper.findById(userId);
    if (user == null) {
      throw new NotFoundException("用户不存在");
    }
    WxPayUnifiedOrderRequest request = WxPayUnifiedOrderRequest.newBuilder()
      .openid(user.getOpenId())
      .outTradeNo(userId)
      .totalFee(new BigDecimal(amount).multiply(new BigDecimal(100)).intValue())
      .body("订单信息")
      .spbillCreateIp(IPs.getRemoteIP(ctx))
      .notifyUrl(wxNotifyUrl)
      .build();
    WxPayUnifiedOrderResult result;
    try {
      result = newWxPayService().unifiedOrder(request);
    } catch (WxPayException ex) {
      throw new RuntimeException("支付失败");
    }
    String prepayId = result.getPrepayId();
    long time = System.currentTimeMillis();
    String timeStamp = Long.toString(time / 1000);
    String nonceStr = String.valueOf(time);
    String pkg = "prepay_id" + result.getPrepayId();
    String signType = "MD5";

    Map<String, String> map = new HashMap<>();
    map.put("appId", wxAppId);
    map.put("timeStamp", timeStamp);
    map.put("nonceStr", nonceStr);
    map.put("signType", signType);
    map.put("package", pkg);
    String paySign = SignUtils.createSign(map, signType, wxMchKey, new String[]{});

    user.setPrepayId(prepayId);
    user.setVip(vip);
    return PayResp.builder()
      .timeStamp(timeStamp)
      .nonceStr(nonceStr)
      .pkg(pkg)
      .paySign(paySign)
      .signType(signType)
      .build();
  }

  private WxPayService newWxPayService() {
    WxPayConfig config = new WxPayConfig();
    config.setAppId(wxAppId);
    config.setMchId(wxMchId);
    config.setMchKey(wxMchKey);
    config.setKeyPath(wxKeyPath);
    WxPayService service = new WxPayServiceImpl();
    service.setConfig(config);
    return service;
  }

  @Data
  @Builder
  public static class PayResp {
    private String timeStamp;
    private String nonceStr;
    private String pkg;
    private String paySign;
    private String signType;
  }
}