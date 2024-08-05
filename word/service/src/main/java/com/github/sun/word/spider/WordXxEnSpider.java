package com.github.sun.word.spider;

import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.spider.Fetcher;
import com.github.sun.spider.spi.JSoupFetcher;
import com.github.sun.spider.spi.XPaths;
import com.github.sun.word.WordAffix;
import com.github.sun.word.WordAffixMapper;
import com.github.sun.word.WordDict;
import com.github.sun.word.WordDictLoader;
import com.ibm.icu.impl.data.ResourceReader;
import lombok.Data;
import org.apache.commons.text.StringEscapeUtils;
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
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class WordXxEnSpider {
  private static final Fetcher fetcher = new JSoupFetcher();
  private static final HtmlCleaner hc = new HtmlCleaner();
  private final static ClassLoader loader = ResourceReader.class.getClassLoader();

  @Resource
  private WordAffixMapper mapper;

  public static void fetchPhrase(WordDict dict, Consumer<WordDict.Phrase> func) {
    try {
      Document node = WordDictLoader.fetchDocument("https://www.xxenglish.com/w6/" + dict.getId());
      List<Node> arr = XPaths.of(node, "//span[@class='CX']").asArray();
      arr.forEach(v -> {
        String name = XPaths.of(v, "./span[@class='YX']").asText();
        String desc = XPaths.of(v, "./span[@class='JX']").asText();
        name = StringEscapeUtils.unescapeHtml4(name);
        desc = StringEscapeUtils.unescapeHtml4(desc);
        if (StringUtils.hasText(name) && StringUtils.hasText(desc)) {
          func.accept(new WordDict.Phrase(name, desc.substring(2)));
        }
      });
    } catch (Exception ex) {
      //do nothing
    }
  }

  public static Set<String> fetchDiffs(WordDict dict) {
    Set<String> set = new HashSet<>();
    try {
      Document node = WordDictLoader.fetchDocument("https://www.xxenglish.com/w7/" + dict.getId());
      List<Node> arr = XPaths.of(node, "//a[@class='sec-trans']").asArray();
      arr.forEach(a -> set.add(a.getTextContent()));
    } catch (Exception ex) {
      //do nothing
    }
    return set;
  }

  // --------------------------------------- affix -----------------------------------
  public void fetchAffix() {
    Arrays.asList("xxen_affix", "xxen_affix_junior", "xxen_affix_middle", "xxen_affix_senior", "xxen_affix_other")
      .forEach(file -> {
        try (InputStream in = loader.getResourceAsStream("affix/" + file + ".json")) {
          assert in != null;
          BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
          String json = reader.lines().collect(Collectors.joining(""));
          List<Affix> affixes = JSON.deserializeAsList(json, Affix.class);

          for (Affix affix : affixes) {
            String html = fetcher.fetch("https://www.xxenglish.com/root/" + affix.getRoots().get(0));
            Document node = new DomSerializer(new CleanerProperties()).createDOM(hc.clean(html));
            List<Node> divs = XPaths.of(node, "//article/div").asArray();
            List<String> roots = new ArrayList<>();
            String indicate = "";
            for (Node div : divs) {
              Node cls = div.getAttributes().getNamedItem("class");
              if (cls.getNodeValue().contains("ras-title")) {
                String s = XPaths.of(div, ".//span[@class='ras-content']/text()").asText();
                indicate = XPaths.of(div, ".//span[@class='ras-indicate']/text()").asText();
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
                rootDesc = StringUtils.hasText(rootDesc) ? rootDesc : indicate;
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
      });
  }

  @Data
  public static class Affix {
    private List<String> roots;
    private String en;
    private String desc;
  }
}
