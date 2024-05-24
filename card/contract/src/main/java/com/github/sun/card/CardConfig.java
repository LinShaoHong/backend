package com.github.sun.card;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class CardConfig {
  private int avaCount;
  private int playLimit;
  private int iosLimit;
  private boolean game;
  private boolean iosCanPay;
  private String iosText;
  private String price;
  private String payText;
  private String shareTitle;
  private String logo;
  private List<Partner> partners;
  private List<Banner> banners;

  public List<Banner> getBanners() {
    return banners == null ? Collections.emptyList() : banners;
  }

  public List<Partner> getPartners() {
    return partners == null ? Collections.emptyList() : partners;
  }

  @Data
  public static class Partner {
    private String name;
    private String logo;
  }

  @Data
  public static class Banner {
    private String src;
    private String qr;
    private String title;
    private String label;
  }
}
