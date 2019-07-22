package com.github.sun.picture;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.foundation.boot.utility.Retry;
import com.github.sun.foundation.boot.utility.SSL;
import com.github.sun.foundation.boot.utility.Strings;
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
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service("picture" + Spider.Processor.SUFFIX)
public class PictureProcessor implements Spider.Processor {
  private static final String USER_AGENT =
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.143 Safari/537.36";
  private static final String PATH = "/opt/static/pictures";

  @Autowired
  private PicService service;

  @Override
  public void process(String source, List<JsonNode> values, Setting setting) {
    List<Pic> pics = JSON.deserializeAsList(values, Pic.class);
    pics.stream().filter(p -> p.getExt() != null).forEach(p -> {
      p.setSource(source);
      p.setHashCode(code(p));
      String mainPath = PATH + "/" + source + "/" + p.getHashCode();
      String detailsPath = mainPath + "/details";
      File detailsDir = new File(detailsPath);
      if (detailsDir.exists() || detailsDir.mkdirs()) {
        Exception e = null;
        try {
          InputStream mainIn = open(p.getOriginalUrl(), setting);
          String mfPath = mainPath + "/main" + p.getExt();
          Files.asByteSink(new File(mfPath)).writeFrom(mainIn);
          p.setPath(mfPath);
          p.getDetails().stream().filter(d -> d.getExt() != null)
            .forEach(d -> {
              try {
                d.setHashCode(code(p, d));
                String dfPath = detailsPath + "/" + d.getHashCode() + d.getExt();
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
        } catch (Exception ex) {
          e = ex;
          log.warn("failed write picture.", ex);
        } finally {
          if (e == null) {
            p.getDetails().removeIf(d -> d.getPath() == null);
            if (!p.getDetails().isEmpty()) {
              try {
                service.save(p);
              } catch (Throwable ex) {
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
      String id = String.valueOf(p.getHashCode());
      Picture picture = Picture.builder()
        .id(id)
        .category(p.getCategory())
        .title(p.getTitle())
        .source(p.getSource())
        .type(p.getType())
        .tags(p.getTags())
        .localPath(p.getPath())
        .originUrl(p.getOriginalUrl())
        .build();
      List<Picture.Detail> details = p.details.stream()
        .filter(d -> d.getPath() != null)
        .map(d -> Picture.Detail.builder()
          .id(picture.getId() + ":" + d.getHashCode())
          .picId(picture.getId())
          .source(p.getSource())
          .originUrl(d.getOriginalUrl())
          .localPath(d.getPath())
          .build())
        .collect(Collectors.toList());
      Picture exist = mapper.findById(id);
      if (exist == null) {
        mapper.insert(picture);
      } else {
        mapper.update(picture);
      }
      Set<String> exists = detailsMapper.findByPicId(id)
        .stream().map(Picture.Detail::getId).collect(Collectors.toSet());
      List<Picture.Detail> updates = details.stream()
        .filter(d -> exists.contains(d.getId())).collect(Collectors.toList());
      if (!updates.isEmpty()) {
        detailsMapper.updateAll(details);
      }
      details.removeIf(d -> exists.contains(d.getId()));
      if (!details.isEmpty()) {
        detailsMapper.insertAll(details);
      }
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

    private String getExt() {
      return getExtension(originalUrl);
    }
  }

  @Data
  private static class Detail {
    private String originalUrl;
    private String path;
    private int hashCode;

    private String getExt() {
      return getExtension(originalUrl);
    }
  }

  private static String getExtension(String url) {
    List<String> normals = Arrays.asList(".jpg", ".jpeg", ".png", ".gif");
    int i = url.lastIndexOf("\\.");
    if (i > 0) {
      String ext = url.substring(i).toLowerCase();
      if (normals.contains(ext)) {
        return ext;
      }
    }
    return null;
  }
}
