package com.github.sun.qm;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.sun.foundation.boot.utility.*;
import com.github.sun.spider.Setting;
import com.github.sun.spider.Spider;
import com.google.common.io.Files;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service("QM_PIC" + Spider.Processor.SUFFIX)
public class PicProcessor implements Spider.Processor {
    @Value("${image.store.dir}")
    private String basePath;

    @Autowired
    private ImgService service;

    @Override
    public int process(String source, List<JsonNode> values, Setting setting, Consumer<Throwable> func) {
        List<Img> pics = JSON.deserializeAsList(values, Img.class);
        AtomicInteger counter = new AtomicInteger(0);
        pics.stream().filter(p -> p.getExt() != null).forEach(p -> {
            p.setSource(source);
            p.setHashCode(code(p.getTitle()));
            String mainPath = basePath + "/" + p.getType() + "/" + p.getHashCode();
            String detailsPath = mainPath + "/details";
            File detailsDir = new File(detailsPath);
            if (detailsDir.exists() || detailsDir.mkdirs()) {
                Exception e = null;
                try {
                    InputStream mainIn = open(p.getOriginalUrl(), setting);
                    String mfPath = mainPath + "/main" + p.getExt();
                    Files.asByteSink(new File(mfPath)).writeFrom(mainIn);
                    p.setPath(mfPath.substring(basePath.length()));
                    p.getDetails().stream().filter(d -> d.getExt() != null)
                            .forEach(d -> {
                                try {
                                    d.setHashCode(code(d.getOriginalUrl()));
                                    String dfPath = detailsPath + "/" + d.getHashCode() + d.getExt();
                                    File file = new File(dfPath);
                                    if (!file.exists()) {
                                        InputStream detailIn = open(d.getOriginalUrl(), setting);
                                        Files.asByteSink(file).writeFrom(detailIn);
                                        d.setPath(dfPath.substring(basePath.length()));
                                    }
                                } catch (Exception ex) {
                                    func.accept(ex);
                                    log.warn("failed write image detail.", ex);
                                }
                            });
                } catch (Exception ex) {
                    e = ex;
                    log.warn("failed write image.", ex);
                } finally {
                    if (e == null) {
                        p.getDetails().removeIf(d -> d.getPath() == null);
                        counter.incrementAndGet();
                        if (!p.getDetails().isEmpty()) {
                            try {
                                service.save(p);
                            } catch (Throwable ex) {
                                func.accept(ex);
                                counter.decrementAndGet();
                                log.error("failed to save images to db.", ex);
                            }
                        }
                    } else {
                        func.accept(e);
                    }
                }
            }
        });
        return counter.get();
    }

    private int code(String s) {
        return Math.abs(Strings.hash(s).hashCode());
    }

    @Service
    public static class ImgService {
        @Autowired
        private GirlMapper mapper;
        @Autowired
        private GirlMapper.Category categoryMapper;

        @Transactional
        public void save(Img p) {
            String id = String.valueOf(p.getHashCode());
            String title = p.getCategory().replace("|", " ");
            String categorySpell = p.getCategory();
            if (!categorySpell.isEmpty()) {
                categorySpell = Stream.of(categorySpell.split("\\|")).map(Pinyins::spell).collect(Collectors.joining("|"));
            }
            Girl girl = Girl.builder()
                    .id(id)
                    .name(p.getTitle())
                    .title(title)
                    .category(p.getCategory())
                    .categorySpell(categorySpell)
                    .type(Girl.Type.PIC)
                    .onService(true)
                    .mainImage(p.getPath())
                    .detailImages(p.details.stream()
                            .map(Detail::getPath)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()))
                    .build();
            Girl exist = mapper.findById(id);
            if (exist == null) {
                mapper.insert(girl);
                updateCategory(girl);
            } else {
                mapper.update(girl);
            }
        }

        private void updateCategory(Girl girl) {
            String tags = girl.getCategory();
            if (tags != null && !tags.isEmpty()) {
                List<Girl.Category> categories = Arrays.stream(tags.split("\\|"))
                        .distinct()
                        .map(name -> {
                            Girl.Type type = girl.getType();
                            String nameSpell = Pinyins.spell(name);
                            return Girl.Category.builder()
                                    .id(type + ":" + nameSpell)
                                    .type(type)
                                    .name(name)
                                    .nameSpell(nameSpell)
                                    .count(1)
                                    .build();
                        })
                        .collect(Collectors.toList());
                categories.forEach(categoryMapper::insertOrUpdate);
            }
        }
    }

    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.143 Safari/537.36";

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
    private static class Img {
        private String source;
        private String category;
        private String type;
        private String title;
        private String originalUrl;
        private String path;
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
        int i = url.lastIndexOf(".");
        if (i > 0) {
            String ext = url.substring(i).toLowerCase();
            if (normals.contains(ext)) {
                return ext;
            }
        }
        return null;
    }
}
