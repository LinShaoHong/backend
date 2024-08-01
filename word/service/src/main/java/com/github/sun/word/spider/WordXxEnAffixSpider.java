package com.github.sun.word.spider;

import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.spider.Fetcher;
import com.github.sun.spider.spi.JSoupFetcher;
import com.github.sun.spider.spi.XPaths;
import com.github.sun.word.WordAffix;
import com.github.sun.word.WordAffixMapper;
import com.ibm.icu.impl.data.ResourceReader;
import lombok.Data;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WordXxEnAffixSpider {
  private static final Fetcher fetcher = new JSoupFetcher();
  private static final HtmlCleaner hc = new HtmlCleaner();
  private final static ClassLoader loader = ResourceReader.class.getClassLoader();

  @Resource
  private WordAffixMapper mapper;

  public void apply() {
    try (InputStream in = loader.getResourceAsStream("affix/xxen_affix_other.json")) {
      assert in != null;
      BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
      String json = reader.lines().collect(Collectors.joining(""));
      List<Affix> affixes = JSON.deserializeAsList(json, Affix.class);

      for (Affix affix : affixes) {
        String html = fetcher.fetch("https://www.xxenglish.com/root/" + affix.getRoots().get(0));
        Document node = new DomSerializer(new CleanerProperties()).createDOM(hc.clean(html));
        List<Node> divs = XPaths.of(node, "//article/div").asArray();
        List<String> roots = new ArrayList<>();
        for (Node div : divs) {
          Node cls = div.getAttributes().getNamedItem("class");
          if (cls.getNodeValue().contains("ras-title")) {
            String s = XPaths.of(div, ".//span[@class='ras-content']/text()").asText();
            roots = Arrays.asList(s.split("，"));
            roots = roots.stream()
              .flatMap(r -> Arrays.stream(r.split(",")).map(v -> v.replaceAll(" ", "")))
              .flatMap(v -> {
                List<String> vs = new ArrayList<>();
                if (v.contains("(")) {
                  int i = v.indexOf("(");
                  int j = v.lastIndexOf(")");
                  String prefix = i > 0 ? v.substring(0, i) : "";
                  String infix = v.substring(i + 1, j);
                  String suffix = j < v.length() - 1 ? v.substring(j + 1) : "";
                  vs.add(prefix + suffix);
                  vs.add(prefix + infix + suffix);
                } else {
                  vs.add(v);
                }
                return vs.stream();
              }).collect(Collectors.toList());
            roots.sort(Comparator.comparingInt(String::length));
          } else {
            String rootDesc = XPaths.of(div, ".//span[@class='ras-td']").asText();
            rootDesc = StringUtils.hasText(rootDesc) ? rootDesc : affix.getEn() + " " + affix.getDesc();
            List<Node> rs = XPaths.of(div, ".//div[contains(@class,'ex-ras')]").asArray();
            for (Node ras : rs) {
              String word = XPaths.of(ras, ".//span[@class='ex-word']").asText();
              String desc = XPaths.of(ras, ".//span[@class='ex-dec']").asText();

              String root = "";
              for (int i = roots.size() - 1; i >= 0; i--) {
                if (word.contains(roots.get(i))) {
                  root = roots.get(i);
                  break;
                }
              }
              System.out.println(word + ":" + root);
              if (StringUtils.hasText(root)) {
                WordAffix v = mapper.findById(word);
                if (v == null) {
                  v = new WordAffix();
                }
                v.setRoot(root);
                v.setRootDesc(rootDesc);
                v.setWordDesc(desc);
                if (v.getId() == null) {
                  v.setId(word);
                  mapper.insert(v);
                } else {
                  mapper.update(v);
                }
              }
            }
          }
        }
      }
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  @Data
  public static class Affix {
    private List<String> roots;
    private String en;
    private String desc;
  }
}
