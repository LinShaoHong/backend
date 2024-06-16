package com.github.sun.card;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.sun.foundation.boot.exception.NotFoundException;
import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.foundation.sql.IdGenerator;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.client.Client;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RefreshScope
@RequiredArgsConstructor
public class CardUserService {
  private static final String WX_URI = "https://api.weixin.qq.com/sns/jscode2session";
  @Value("${wx.appId}")
  private String wxAppId;
  @Value("${wx.secret}")
  private String wxSecret;
  @Value("${config.iosLimit}")
  private int iosLimit;
  @Value("${config.iosCanPay}")
  private boolean iosCanPay;
  @Value("${config.iosGrantCount}")
  private int iosGrantCount;
  private final Client client;
  private final CardUserMapper mapper;
  private final CardCodeService codeService;
  private final CardUserDefService defService;
  private final CardConfig config;

  @Transactional
  public UserResp wxLogin(String code,
                          String shareUserId,
                          String os,
                          String ip,
                          String partner,
                          String location) {
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
      int vip = 0;
      if (StringUtils.hasText(partner)) {
        if (config.getPartners().stream().anyMatch(v -> Objects.equals(v.getName(), partner))) {
          vip = 1;
        }
      }
      CardUser user = mapper.byOpenId(openId);
      if (user == null) {
        CardUser shareUser = null;
        if (StringUtils.hasText(shareUserId)) {
//        if (StringUtils.hasText(shareUserId) && !iosCanPay) {
          shareUser = mapper.findById(shareUserId);
//          if (shareUser != null) {
//            if (shareUser.getPlayCount() > iosLimit) {
//              shareUser.setPlayCount(iosLimit);
//            }
//            shareUser.setPlayCount(shareUser.getPlayCount() - iosGrantCount);
//            mapper.update(shareUser);
//          }
        }

        code = codeService.genCode("USER");
        String userId = IdGenerator.next();
        user = CardUser.builder()
          .id(userId)
          .code("wx_" + code)
          .shareCode(shareUser == null ? null : shareUser.getCode())
          .os(os)
          .avatar(new Random().nextInt(40) + 1)
          .vip(vip)
          .nickname("昵称" + code)
          .openId(n.asText())
          .ip(ip)
          .location(location)
          .build();
        defService.init(userId);
        mapper.replace(user);
      } else {
        if ((StringUtils.hasText(os) && !Objects.equals(user.getOs(), os)) || (user.getVip() != vip)) {
          user.setOs(os);
          if (vip > 0) {
            user.setVip(vip);
          }
          mapper.update(user);
        }
      }
      return UserResp.from(user);
    }
    throw new BadRequestException("获取OpenId失败");
  }

  public UserResp byId(String id, String os) {
    CardUser user = mapper.findById(id);
    if (user != null && !StringUtils.hasText(user.getOs()) && StringUtils.hasText(os)) {
      user.setOs(os);
      mapper.update(user);
    }
    return user == null ? null : UserResp.from(user);
  }

  @Transactional
  public void inc(String id, boolean hks) {
    if (hks) {
      mapper.inc(id);
    } else {
      mapper.incLover(id);
    }
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

  public List<UserResp> invited(String code) {
    if (StringUtils.hasText(code)) {
      return mapper.byShareCode(code).stream().map(UserResp::from).collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  @Data
  @Builder
  public static class UserResp {
    private String id;
    private String code;
    private String shareCode;
    private String openId;
    private int avatar;
    private String nickname;
    private int playCount;
    private int loverPlayCount;
    private int vip;

    public static UserResp from(CardUser user) {
      return UserResp.builder()
        .id(user.getId())
        .code(user.getCode())
        .shareCode(user.getShareCode())
        .openId(user.getOpenId())
        .avatar(user.getAvatar())
        .nickname(user.getNickname())
        .playCount(user.getPlayCount())
        .loverPlayCount(user.getLoverPlayCount())
        .vip(user.getVip())
        .build();
    }
  }

  @Slf4j
  @Configuration
  public static class CardConfiguration {
    @Bean
    @ConfigurationProperties("config")
    public CardConfig config() {
      return new CardConfig();
    }
  }
}