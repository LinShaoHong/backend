package com.github.sun.card;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class CardConfig {
  private int avaCount;
  private int playLimit;
  private int loverPlayLimit;
  private int iosLimit;
  private boolean game;
  private boolean iosCanPay;
  private String iosText;
  private String price;
  private String payText;
  private String shareTitle;
  private String logo;
  private String roomTitle;
  private String loverPlayTitle;
  private TopTab topTab;
  private More more;
  private List<Lover.Card> loverCards;
  private boolean noLover;
  private List<Partner> partners;
  private boolean toHks;
  private List<Banner> banners;

  public List<Banner> getBanners() {
    return banners == null ? Collections.emptyList() : banners;
  }

  public List<Partner> getPartners() {
    return partners == null ? Collections.emptyList() : partners;
  }

  @Data
  public static class TopTab {
    private String hks;
    private String lover;
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

  @Data
  public static class More {
    private Hks hks;
    private Lover lover;
  }

  @Data
  public static class Hks {
    private String defTitle;
    private String defContent;
    private String battleTitle;
    private String battleContent;
  }

  @Data
  public static class Lover {
    private String defTitle;
    private String defContent;
    private String battleTitle;
    private String battleContent;

    @Data
    public static class Card {
      private String name;
      private String type;
      private boolean open;
      private boolean visible;
    }
  }
}
