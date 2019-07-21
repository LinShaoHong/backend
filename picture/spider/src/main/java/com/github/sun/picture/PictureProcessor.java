package com.github.sun.picture;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.sun.foundation.boot.utility.*;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.foundation.sql.factory.SqlBuilderFactory;
import com.github.sun.picture.config.PicTransactional;
import com.github.sun.picture.mapper.PictureDetailsMapper;
import com.github.sun.picture.mapper.PictureMapper;
import com.github.sun.spider.Setting;
import com.github.sun.spider.Spider;
import com.google.common.io.Files;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service("picture" + Spider.Processor.SUFFIX)
public class PictureProcessor implements Spider.Processor {
  private static final String USER_AGENT =
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.143 Safari/537.36";

  private final String PATH = "/opt/static/pictures";
  private final SqlBuilder.Factory factory = SqlBuilderFactory.mysql();

  @Autowired
  private PicService service;
  @Resource
  private PictureMapper mapper;

  @Override
  public void process(String source, List<JsonNode> values, Setting setting) {
    SqlBuilder sb = factory.create();
    List<Pic> pics = JSON.deserializeAsList(values, Pic.class);
    Set<String> codes = pics.stream().map(this::code).map(String::valueOf).collect(Collectors.toSet());
    SqlBuilder.Template template = sb.from(Picture.class)
      .where(sb.field("id").in(codes))
      .template();
    Set<String> exists = mapper.findByTemplate(template).stream().map(Picture::getId).collect(Collectors.toSet());
    pics = pics.stream().filter(p -> !exists.contains(String.valueOf(code(p)))).collect(Collectors.toList());
    if (!pics.isEmpty()) {
      process(source, setting, pics);
    }
  }

  private void process(String source, Setting setting, List<Pic> pics) {
    pics.forEach(p -> {
      p.setSource(source);
      p.setHashCode(code(p));
      String mainPath = PATH + "/" + source + "/" + p.getHashCode();
      String detailsPath = mainPath + "/details";
      File mainDir = new File(mainPath);
      File detailsDir = new File(detailsPath);
      InputStream mainIn;
      try {
        mainIn = open(p.getOriginalUrl(), setting);
      } catch (Exception ex) {
        log.warn("failed to get stream from url: " + p.getOriginalUrl(), ex);
        return;
      }
      if (!mainDir.exists() && detailsDir.mkdirs()) {
        Exception e = null;
        try {
          String mfPath = mainPath + "/main.jpg";
          Files.asByteSink(new File(mfPath)).writeFrom(mainIn);
          p.setPath(mfPath);
          p.getDetails().forEach(d -> {
            try {
              d.setHashCode(code(p, d));
              String dfPath = detailsPath + "/" + d.getHashCode() + ".jpg";
              File file = new File(dfPath);
              if (!file.exists()) {
                InputStream detailIn = open(d.getOriginalUrl(), setting);
                Files.asByteSink(file).writeFrom(detailIn);
                d.setPath(dfPath);
              }
            } catch (Exception ex) {
              log.warn("failed write picture detail.", ex);
              // do nothing
            }
          });
        } catch (IOException ex) {
          e = ex;
          log.warn("failed write picture.", ex);
        } finally {
          if (e != null) {
            IO.delete(mainDir);
          } else {
            p.getDetails().removeIf(d -> d.getPath() == null);
            if (p.getDetails().isEmpty()) {
              IO.delete(mainDir);
            } else {
              try {
                service.save(p);
              } catch (Throwable ex) {
                IO.delete(mainDir);
                log.error("failed to save pictures to db.", ex);
              }
            }
          }
        }
      }
    });
  }

  private int code(String s) {
    return Math.abs(Strings.hash(s).hashCode());
  }

  private int code(Pic p, Detail d) {
    return code(p.getSource() + ":" + d.getOriginalUrl());
  }

  private int code(Pic p) {
    return code(p.getSource() + ":" + p.getOriginalUrl());
  }

  @Service
  public static class PicService {
    @Resource
    private PictureMapper mapper;
    @Resource
    private PictureDetailsMapper detailsMapper;

    @PicTransactional
    public void save(Pic p) {
      Date now = new Date();
      Picture picture = Picture.builder()
        .id(String.valueOf(p.getHashCode()))
        .category(p.getCategory())
        .title(p.getTitle())
        .source(p.getSource())
        .type(p.getType())
        .tags(p.getTags())
        .localPath(p.getPath())
        .originUrl(p.getOriginalUrl())
        .createTime(now)
        .updateTime(now)
        .build();
      List<Picture.Detail> details = p.details.stream()
        .filter(d -> d.getPath() != null)
        .map(d -> Picture.Detail.builder()
          .id(picture.getId() + ":" + d.getHashCode())
          .picId(picture.getId())
          .source(p.getSource())
          .originUrl(d.getOriginalUrl())
          .localPath(d.getPath())
          .createTime(now)
          .updateTime(now)
          .build())
        .collect(Collectors.toList());
      mapper.insert(picture);
      detailsMapper.insertAll(details);
    }
  }

  private InputStream open(String url, Setting setting) throws Exception {
    return Retry.execute(setting.getRetryCount(), setting.getRetryDelays(), () -> {
      URL u = new URL(url);
      URLConnection connection = u.openConnection();
      if (url.toLowerCase().startsWith("https://")) {
        ((HttpsURLConnection) connection).setHostnameVerifier((s, sslSession) -> false);
        ((HttpsURLConnection) connection).setSSLSocketFactory(SSL.getContext().getSocketFactory());
      }
      connection.setRequestProperty("User-Agent", USER_AGENT);
      return u.openStream();
    });
  }

  @Data
  private static class Pic {
    private String source;
    private String category;
    private String type;
    private String title;
    private String originalUrl;
    private String path;
    private String tags;
    private int hashCode;
    private List<Detail> details;
  }

  @Data
  private static class Detail {
    private String originalUrl;
    private String path;
    private int hashCode;
  }
}
