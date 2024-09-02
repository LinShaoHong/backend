package com.github.sun.word.spider;

import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.foundation.boot.utility.Strings;
import com.github.sun.spider.XPaths;
import com.github.sun.word.WordDict;
import com.github.sun.word.WordDictLoader;
import com.github.sun.word.loader.WordLoaderAffix;
import com.github.sun.word.loader.WordLoaderAffixMapper;
import com.ibm.icu.impl.data.ResourceReader;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@Service
public class WordXxEnSpider {
  private final static ClassLoader loader = ResourceReader.class.getClassLoader();

  @Resource
  private WordLoaderAffixMapper mapper;

  public static void fetchDerivative(String word, Consumer<Set<String>> func) {
    try {
      Set<String> words = new LinkedHashSet<>();
      Document node = WordDictLoader.fetchDocument("https://www.xxenglish.com/w1/" + word);
      List<Node> arr = XPaths.of(node, "//article/p").asArray();
      int j = -1;
      for (int i = 0; i < arr.size(); i++) {
        if (arr.get(i).getTextContent().contains("词形变化：")) {
          j = i + 1;
          break;
        }
      }
      if (j > 0) {
        XPaths.of(arr.get(j), "./a").asArray().forEach(a -> words.add(a.getTextContent()));
      }
      func.accept(words);
    } catch (Throwable ex) {
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

  public static boolean has(String word) {
    try {
      Document node = WordDictLoader.fetchDocument("https://www.xxenglish.com/wd/" + word);
      return XPaths.of(node, "//div[@class='guess-title']").asArray().isEmpty();
    } catch (Exception ex) {
      //do nothing
    }
    return true;
  }

  // --------------------------------------- affix -----------------------------------
  public void fetchAffix() {
    try (InputStream in = loader.getResourceAsStream("affix/1.json")) {
      assert in != null;
      BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
      String line = reader.readLine();
      while (StringUtils.hasText(line)) {
        String word = JSON.asJsonNode(line).get("word").asText("");
        WordLoaderAffix affix = mapper.findById(word);
        if (affix == null) {
          affix = new WordLoaderAffix();
        }
        String content = JSON.asJsonNode(line).get("content").asText("");
        Strings.Parser parser = Strings.newParser().set(content);
        if (StringUtils.hasText(content)) {
          if (content.contains("词根分析")) {
            parser.next(Pattern.compile("(?:(?!词根分析).)+", Pattern.DOTALL));
            parser.next(Pattern.compile("词根分析", Pattern.DOTALL));
            parser.next(Pattern.compile("\\*+"));
            parser.next(Pattern.compile("】"));
            parser.next(Pattern.compile("——"));
            parser.next(Pattern.compile("_"));
            parser.next(Pattern.compile(":"));
            parser.next(Pattern.compile("："));
            parser.next(Pattern.compile("\\*+"));
            parser.next(Pattern.compile("[\\s\\n]+"));
            parser.next(Pattern.compile("[^\\n]+"));
            String rootDesc = parser.processed();
            affix.setGptRoot(rootDesc);
          }
          if (content.contains("词缀分析")) {
            parser.next(Pattern.compile("(?:(?!词缀分析).)+", Pattern.DOTALL));
            parser.next(Pattern.compile("词缀分析", Pattern.DOTALL));
            parser.next(Pattern.compile("\\*+"));
            parser.next(Pattern.compile("】"));
            parser.next(Pattern.compile("——"));
            parser.next(Pattern.compile("_"));
            parser.next(Pattern.compile(":"));
            parser.next(Pattern.compile("："));
            parser.next(Pattern.compile("\\*+"));
            parser.next(Pattern.compile("[\\s\\n]+"));
            parser.next(Pattern.compile("[^\\n]+"));
            String affixDesc = parser.processed();
            affix.setGptAffix(affixDesc);
          }
        }
        if (StringUtils.hasText(affix.getGptRoot()) || StringUtils.hasText(affix.getGptAffix())) {
          if (affix.getId() == null) {
            affix.setId(word);
            mapper.insert(affix);
          } else {
            mapper.update(affix);
          }
        }
        line = reader.readLine();
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
