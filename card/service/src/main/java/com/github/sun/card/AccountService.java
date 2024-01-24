package com.github.sun.card;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AccountService {
  @Value("${wx.appId}")
  private String appId;
  @Value("${wx.secret}")
  private String secret;

  public String getOpenIdByCode(String code) {

    return null;
  }
}