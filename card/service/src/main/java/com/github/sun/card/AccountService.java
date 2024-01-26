package com.github.sun.card;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import javax.ws.rs.client.Client;

@Service
@RefreshScope
@RequiredArgsConstructor
public class AccountService {
  private static final String WX_URI = "https://api.weixin.qq.com/sns/jscode2session";
  @Value("${wx.appId}")
  private String wxAppId;
  @Value("${wx.secret}")
  private String wxSecret;
  private final Client client;

  public JsonNode wxLogin(String code) {
    return client
      .target(WX_URI)
      .queryParam("appid", wxAppId)
      .queryParam("secret", wxSecret)
      .queryParam("js_code", code)
      .queryParam("grant_type", "authorization_code")
      .request()
      .get()
      .readEntity(JsonNode.class);
  }
}