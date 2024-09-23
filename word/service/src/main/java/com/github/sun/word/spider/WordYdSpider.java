package com.github.sun.word.spider;

import com.github.sun.spider.XPaths;
import com.github.sun.word.WordDict;
import com.github.sun.word.WordDictLoader;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class WordYdSpider {
    public static void main(String[] args) {
        try {
            String w = "liked";
            Document node = WordDictLoader.fetchDocument("https://www.merriam-webster.com/dictionary/" + w);
            List<Node> arr = XPaths.of(node, "//div[@class='entry-word-section-container']").asArray();
            arr.forEach(a -> {//adjective
                System.out.println(XPaths.of(a, ".//h1[@class='hword']").asText());
                System.out.println(XPaths.of(a, ".//h2[@class='parts-of-speech']/a").asText());
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void fetchPhonetic(WordDict dict) {
        Document node = WordDictLoader.fetchDocument("https://dict.youdao.com/result?lang=en&word=" + dict.getId());
        List<Node> arr = XPaths.of(node, "//div[@class='per-phone']").asArray();
        if (!arr.isEmpty()) {
            String uk = XPaths.of(arr.get(0), ".//span[@class='phonetic']/text()").asText();
            uk = StringEscapeUtils.unescapeHtml4(uk);
            dict.setUkPhonetic(uk);
        }
        if (arr.size() > 1) {
            String us = XPaths.of(arr.get(1), ".//span[@class='phonetic']/text()").asText();
            us = StringEscapeUtils.unescapeHtml4(us);
            dict.setUsPhonetic(us);
        }
    }

    @SuppressWarnings("Duplicates")
    public static Set<String> fetchMeaning(WordDict dict) {
        Set<String> set = new LinkedHashSet<>();
        try {
            Document node = WordDictLoader.fetchDocument("https://dict.youdao.com/result?lang=en&word=" + dict.getId());
            List<Node> arr = XPaths.of(node, "//li[@class='word-exp']").asArray();
            arr.forEach(v -> {
                String text = XPaths.of(v, "./span[@class='pos']").asText();
                if ("n.".equals(text)) {
                    set.add("noun");
                }
                if ("v.".equals(text)) {
                    set.add("verb");
                }
                if ("vt.".equals(text)) {
                    set.add("transitiveVerb");
                }
                if ("vi.".equals(text)) {
                    set.add("intransitiveVerb");
                }
                if ("aux.".equals(text)) {
                    set.add("auxiliaryVerb");
                }
                if ("modal.".equals(text)) {
                    set.add("modalVerb");
                }
                if ("adv.".equals(text)) {
                    set.add("adverb");
                }
                if ("adj.".equals(text)) {
                    set.add("adjective");
                }
                if ("prep.".equals(text)) {
                    set.add("preposition");
                }
                if ("pron.".equals(text)) {
                    set.add("pronoun");
                }
                if ("conj.".equals(text)) {
                    set.add("conjunction");
                }
                if ("art.".equals(text)) {
                    set.add("article");
                }
                if ("int.".equals(text)) {
                    set.add("interjection");
                }
                if ("num.".equals(text)) {
                    set.add("numeral");
                }
                if ("det.".equals(text)) {
                    set.add("determiner");
                }
            });
        } catch (Exception ex) {
            //do nothing
        }
        return set;
    }

    @SuppressWarnings("Duplicates")
    public static void fetchInflection(WordDict dict) {
        try {
            Document node = WordDictLoader.fetchDocument("https://dict.youdao.com/result?lang=en&word=" + dict.getId());
            WordDict.Inflection inflection = dict.getInflection();
            List<Node> arr = XPaths.of(node, "//li[@class='word-wfs-cell-less']").asArray();
            for (Node n : arr) {
                String name = XPaths.of(n, ".//span[@class='wfs-name']").asText();
                String words = XPaths.of(n, ".//span[@class='transformation']").asText();
                name = StringEscapeUtils.unescapeHtml4(name);
                words = StringEscapeUtils.unescapeHtml4(words);
                List<String> ws = Arrays.asList(words.split("或"));
                switch (name) {
                    case "复数":
                        inflection.setPlural(ws);
                        break;
                    case "第三人称单数":
                        inflection.setThirdPresent(ws);
                        break;
                    case "现在分词":
                        inflection.setProgressive(ws);
                        break;
                    case "过去式":
                        inflection.setPast(ws);
                        break;
                    case "过去分词":
                        inflection.setPerfect(ws);
                        break;
                    case "比较级":
                        inflection.setComparative(ws);
                        break;
                    case "最高级":
                        inflection.setSuperlative(ws);
                        break;
                    default:
                        break;
                }
            }
            dict.setInflection(inflection);
        } catch (Exception ex) {
            //do nothing
        }
    }
}