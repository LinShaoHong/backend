package com.github.sun.card;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.foundation.sql.IdGenerator;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.ws.rs.BadRequestException;
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
  private final UserMapper mapper;

  public UserResp wxLogin(String code) {
    String resp = client
      .target(WX_URI)
      .queryParam("appid", wxAppId)
      .queryParam("secret", wxSecret)
      .queryParam("js_code", code)
      .queryParam("grant_type", "authorization_code")
      .request()
      .get()
      .readEntity(String.class);
    JsonNode node = JSON.asJsonNode(resp);
    JsonNode n = node.get("openid");
    if (n != null && StringUtils.hasText(n.asText())) {
      String openId = n.asText();
      CardUser user = mapper.byOpenId(openId);
      if (user == null) {
        user = CardUser.builder()
          .id(IdGenerator.next())
          .nickname("微信用户")
          .openId(n.asText())
          .build();
        mapper.insert(user);
      }
      return UserResp.from(user);
    }
    throw new BadRequestException("获取OpenId失败");
  }

  public UserResp byId(String id) {
    CardUser user = mapper.findById(id);
    return UserResp.from(user);
  }

  public void inc(String id) {
    mapper.inc(id);
  }

  public void updateNickname(String id, String nickname) {
    mapper.updateNickname(id, nickname);
  }

  @Data
  @Builder
  public static class UserResp {
    private String id;
    private String openId;
    private String nickname;
    private int playCount;
    private boolean vip;

    public static UserResp from(CardUser user) {
      return UserResp.builder()
        .id(user.getId())
        .openId(user.getOpenId())
        .nickname(user.getNickname())
        .playCount(user.getPlayCount())
        .vip(user.isVip())
        .build();
    }
  }
}