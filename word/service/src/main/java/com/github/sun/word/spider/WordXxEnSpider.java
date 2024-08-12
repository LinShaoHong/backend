package com.github.sun.word.spider;

import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.foundation.boot.utility.Strings;
import com.github.sun.spider.XPaths;
import com.github.sun.word.WordAffix;
import com.github.sun.word.WordAffixMapper;
import com.github.sun.word.WordDict;
import com.github.sun.word.WordDictLoader;
import com.ibm.icu.impl.data.ResourceReader;
import lombok.Data;
import org.apache.commons.text.StringEscapeUtils;
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
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@Service
public class WordXxEnSpider {
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

  public static boolean has(String word) {
    try {
      Document node = WordDictLoader.fetchDocument("https://www.iciba.com/word?w=" + word);
      return !XPaths.of(node, "//div[@class='guess-title']").asArray().isEmpty();
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
        WordAffix affix = mapper.findById(word);
        if (affix == null) {
          affix = new WordAffix();
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

  public static void main(String[] args) {
    String content = "{\"word\":\"dignity\",\"content\":\"_分析词义_\\n\\n  \\n\\n\\\"Dignity\\\" 是一个英语单词，主要指的是尊严，庄重，尊贵之类的意义。这体现了一个人或实体的价值，应受到尊重和敬仰的特质。\\n\\n  \\n\\n_列举例句_\\n\\n  \\n\\n1.  Everyone deserves to be treated with dignity. (每个人都应得到尊严的对待。)\\n2.  He maintained his dignity in the face of his illness. (他面对疾病时仍保持了尊严。)\\n3.  She is a lady of great dignity. (她是一个非常有尊严的女士。)\\n\\n  \\n\\n_词根分析_\\n\\n  \\n\\n\\\"Dignity\\\" 的词根来自拉丁语 \\\"dignitas\\\"，意为 \\\"尊贵\\\"。从这个词根衍生出的其他单词有 \\\"dignified\\\"（有尊严的）和 \\\"indignity\\\"（失去尊严）等。\\n\\n  \\n\\n_词缀分析_\\n\\n  \\n\\n\\\"Dignity\\\" 的前缀 \\\"di-\\\" 来自拉丁语，表示 \\\"双重\\\" 的意思，而 \\\"-ity\\\" 是一个表示抽象名词的后缀。其他使用了 \\\"-ity\\\" 后缀的词有 \\\"ability\\\"（能力）,\\\"reality\\\"（现实）,\\\"equality\\\"（平等）等。\\n\\n  \\n\\n_发展历史和文化背景_\\n\\n  \\n\\n这个词出现在14世纪的英语中，源自拉丁文的 \\\"dignitas\\\"。在欧美文化中，\\\"dignity\\\" 是一个非常重要的概念，与人权，自由，公平，公正等概念相连。\\n\\n  \\n\\n_单词变形_\\n\\n  \\n\\n*   形容词： Dignified (有尊严的)\\n*   名词复数： Dignities (尊严)\\n\\n  \\n\\n固定搭配有 \\\"dignity of work\\\"（工作的尊严）\\n\\n  \\n\\n_记忆辅助_\\n\\n  \\n\\n道可道，非常道。名可名，非常名。无，名天地之始；有，名万物之母。故常无，欲以观其妙；常有，欲以观其徼。此两者，同出而异名，同谓之玄。玄之又玄，众妙之门。开始\\\"道\\\"字，可理解为象征尊严的开始。通过这种方式，也许可以帮助记住 \\\"dignity\\\" 这个单词。\\n\\n  \\n\\n_小故事_\\n\\n  \\n\\nOnce upon a time, a wise king ruled a large kingdom. Despite the power, he treated every citizen with dignity. He believed that every person’s value lied in their dignity, and it should be respected.\\n\\n  \\n\\n曾经，智慧的国王统治着一个庞大的王国。尽管有着权力，他对待每一个公民都充满了尊严。他坚信每个人的价值在于他们的尊严，这应该被尊重。\"}";
    content = JSON.asJsonNode(content).get("content").asText("");
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
        System.out.println(rootDesc);
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
        System.out.println(affixDesc);
      }
    }
  }

  @Data
  public static class Affix {
    private List<String> roots;
    private String en;
    private String desc;
  }
}
