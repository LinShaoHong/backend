package com.github.sun.card;

import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderV3Request;
import com.github.binarywang.wxpay.bean.result.WxPayUnifiedOrderV3Result;
import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import com.github.binarywang.wxpay.service.impl.WxPayServiceImpl;
import com.github.sun.foundation.boot.exception.NotFoundException;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.github.binarywang.wxpay.bean.result.enums.TradeTypeEnum.JSAPI;

@Service
@RequiredArgsConstructor
public class CardPayService {
  private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyMMddHHmmss");

  @Value("${wx.appId}")
  private String wxAppId;
  @Value("${wx.pay.mchId}")
  private String wxMchId;
  @Value("${wx.pay.api3Key}")
  private String wxApi3Key;
  @Value("${wx.pay.certNo}")
  private String wxCertNo;
  @Value("${wx.pay.certPath}")
  private String wxCertPath;
  @Value("${wx.pay.notifyUrl}")
  private String wxNotifyUrl;
  private final CardUserMapper mapper;

  @Transactional
  public PayResp wxPay(String userId, String amount) {
    CardUser user = mapper.findById(userId);
    if (user == null) {
      throw new NotFoundException("用户不存在");
    }
    WxPayUnifiedOrderV3Request request = new WxPayUnifiedOrderV3Request();
    String date = FORMATTER.format(new Date());
    request.setOutTradeNo((date + userId.hashCode()).replace("-", ""));
    WxPayUnifiedOrderV3Request.Amount fen = new WxPayUnifiedOrderV3Request.Amount();
    fen.setTotal(new BigDecimal(amount).multiply(new BigDecimal(100)).intValue());
    request.setAmount(fen);
    request.setNotifyUrl(wxNotifyUrl);
    request.setDescription("微信支付");
    WxPayUnifiedOrderV3Request.Payer payer = new WxPayUnifiedOrderV3Request.Payer();
    payer.setOpenid(user.getOpenId());
    request.setPayer(payer);

    WxPayUnifiedOrderV3Result.JsapiResult jsapi;
    try {
      jsapi = newWxPayService().createOrderV3(JSAPI, request);
    } catch (WxPayException ex) {
      throw new RuntimeException("支付失败");
    }

    String prepayId = jsapi.getPackageValue();
    user.setPrepayId(prepayId);
    mapper.update(user);
    return PayResp.builder()
      .timeStamp(jsapi.getTimeStamp())
      .nonceStr(jsapi.getNonceStr())
      .pkg(prepayId)
      .paySign(jsapi.getPaySign())
      .signType(jsapi.getSignType())
      .build();
  }

  private WxPayService newWxPayService() {
    WxPayConfig config = new WxPayConfig();
    config.setAppId(wxAppId);
    config.setMchId(wxMchId);
    config.setApiV3Key(wxApi3Key);
    config.setSignType("MD5");
    config.setCertSerialNo(wxCertNo);
    config.setKeyPath(wxCertPath + "/apiclient_cert.p12");
    config.setPrivateCertPath(wxCertPath + "/apiclient_cert.pem");
    config.setPrivateKeyPath(wxCertPath + "/apiclient_key.pem");
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