package com.github.sun.card;

import com.github.sun.foundation.rest.AbstractResource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;

@Path("/config")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CardConfigResource extends AbstractResource {
  private final Config config;

  @Inject
  public CardConfigResource(Config config) {
    this.config = config;
  }

  @GET
  public SingleResponse<Config> get() {
    return responseOf(config);
  }

  @Slf4j
  @Configuration
  public static class CardConfiguration {
    @Bean
    @ConfigurationProperties("config")
    public Config config() {
      return new Config();
    }
  }


  @Data
  public static class Config {
    private int avaCount;
    private int playLimit;
    private int battleLimit;
    private boolean canBattle;
    private boolean iosCanMore;
    private String price;
    private String payText;
    private String shareTitle;
    private String logo;
    private List<Banner> banners;

    public List<Banner> getBanners() {
      return banners == null ? Collections.emptyList() : banners;
    }
  }

  @Data
  public static class Banner {
    private String src;
    private String qr;
    private String title;
    private String label;
  }
}