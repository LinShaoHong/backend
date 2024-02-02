package com.github.sun.card;

import com.github.sun.foundation.rest.AbstractResource;
import lombok.Builder;
import lombok.Data;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.List;

@Path("/config")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ConfigResource extends AbstractResource {
  @GET
  public SingleResponse<Config> get(@QueryParam("code") String code) {
    Banner banner1 = Banner.builder()
      .src("/static/banner/1.jpg")
      .qr("/static/qr.jpg")
      .title("公众号二维码")
      .label("长按识别二维码，打开公众号")
      .build();
    Banner banner2 = Banner.builder()
      .src("/static/banner/2.jpg")
      .qr("/static/qr.jpg")
      .title("公众号二维码")
      .label("长按识别二维码，打开公众号")
      .build();
    return responseOf(Config.builder()
      .cardCount(5)
      .playLimit(5)
      .price("2.99")
      .payText("<div>aaaaa<div>")
      .banners(Arrays.asList(banner1,banner2))
      .build());
  }

  @Data
  @Builder
  public static class Config {
    private int cardCount;
    private int playLimit;
    private String price;
    private String payText;
    private List<Banner> banners;
  }

  @Data
  @Builder
  public static class Banner {
    private String src;
    private String qr;
    private String title;
    private String label;
  }
}