package com.github.sun.card;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.sun.foundation.boot.exception.NotFoundException;
import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.foundation.sql.IdGenerator;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.client.Client;
import java.util.Objects;
import java.util.Random;

@Service
@RefreshScope
@RequiredArgsConstructor
public class CardUserService {
  private static final String WX_URI = "https://api.weixin.qq.com/sns/jscode2session";
  @Value("${wx.appId}")
  private String wxAppId;
  @Value("${wx.secret}")
  private String wxSecret;
  private final Client client;
  private final CardUserMapper mapper;
  private final CardCodeService codeService;
  private final CardUserDefService defService;

  @Transactional
  public UserResp wxLogin(String code, String ip, String location) {
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
        code = codeService.genCode("USER");
        String userId = IdGenerator.next();
        user = CardUser.builder()
          .id(userId)
          .code("wx_" + code)
          .avatar(new Random().nextInt(40) + 1)
          .vip(0)
          .nickname("微信用户-" + code)
          .openId(n.asText())
          .ip(ip)
          .location(location)
          .build();
        defService.init(userId);
        mapper.insert(user);
      }
      return UserResp.from(user);
    }
    throw new BadRequestException("获取OpenId失败");
  }

  public UserResp byId(String id) {
    CardUser user = mapper.findById(id);
    return user == null ? null : UserResp.from(user);
  }

  @Transactional
  public void inc(String id) {
    mapper.inc(id);
  }

  @Transactional
  public void vip(String id, String prepayId, int vip) {
    CardUser user = mapper.findById(id);
    if (user == null) {
      throw new NotFoundException("找不到用户");
    }
    if (!Objects.equals(prepayId, user.getPrepayId())) {
      throw new BadRequestException("用户未支付");
    }
    mapper.vip(id, vip);
  }

  @Transactional
  public void updateNickname(String id, String nickname) {
    mapper.updateNickname(id, nickname);
  }

  @Transactional
  public void updateAvatar(String id, int avatar) {
    mapper.updateAvatar(id, avatar);
  }

  @Data
  @Builder
  public static class UserResp {
    private String id;
    private String code;
    private String openId;
    private int avatar;
    private String nickname;
    private int playCount;
    private int vip;

    public static UserResp from(CardUser user) {
      return UserResp.builder()
        .id(user.getId())
        .code(user.getCode())
        .openId(user.getOpenId())
        .avatar(user.getAvatar())
        .nickname(user.getNickname())
        .playCount(user.getPlayCount())
        .vip(user.getVip())
        .build();
    }
  }
}