package com.github.sun.word.spider;

import com.github.sun.spider.XPaths;
import com.github.sun.word.WordDict;
import com.github.sun.word.WordDictLoader;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.*;

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
                if ("v.".equals(text) || (text.contains("vt.") && text.contains("vi."))) {
                    set.add("verb");
                }
                if (text.equals("vt.")) {
                    set.add("transitiveVerb");
                }
                if (text.equals("vi.")) {
                    set.add("intransitiveVerb");
                }
                if (text.startsWith("aux")) {
                    set.add("auxiliaryVerb");
                }
                if (text.startsWith("modal")) {
                    set.add("modalVerb");
                }
                if (text.startsWith("adv")) {
                    set.add("adverb");
                }
                if (text.startsWith("adj")) {
                    set.add("adjective");
                }
                if (text.startsWith("prep")) {
                    set.add("preposition");
                }
                if (text.startsWith("pron")) {
                    set.add("pronoun");
                }
                if (text.startsWith("conj")) {
                    set.add("conjunction");
                }
                if (text.startsWith("art")) {
                    set.add("article");
                }
                if (text.startsWith("int")) {
                    set.add("interjection");
                }
                if (text.startsWith("num")) {
                    set.add("numeral");
                }
                if (text.startsWith("det")) {
                    set.add("determiner");
                }
                if (text.startsWith("abbr")) {
                    set.add("abbreviation");
                }
            });
        } catch (Exception ex) {
            //do nothing
        }
        List<String> ret = new ArrayList<>(set);
        if (ret.contains("verb")) {
            int i = ret.indexOf("verb");
            if (ret.contains("transitiveVerb") && !ret.contains("intransitiveVerb")) {
                ret.set(i, "intransitiveVerb");
            }
            if (ret.contains("intransitiveVerb") && !ret.contains("transitiveVerb")) {
                ret.set(i, "transitiveVerb");
            }
        }
        return set;
    }

    @SuppressWarnings("Duplicates")
    public static void fetchInflection(WordDict dict, List<String> ms) {
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
                        if (ms.contains("noun")) {
                            inflection.setPlural(ws);
                        }
                        break;
                    case "第三人称单数":
                        if (ms.stream().anyMatch(s -> s.equals("verb") || s.endsWith("Verb"))) {
                            inflection.setThirdPresent(ws);
                        }
                        break;
                    case "现在分词":
                        if (ms.stream().anyMatch(s -> s.equals("verb") || s.endsWith("Verb"))) {
                            inflection.setProgressive(ws);
                        }
                        break;
                    case "过去式":
                        if (ms.stream().anyMatch(s -> s.equals("verb") || s.endsWith("Verb"))) {
                            inflection.setPast(ws);
                        }
                        break;
                    case "过去分词":
                        if (ms.stream().anyMatch(s -> s.equals("verb") || s.endsWith("Verb"))) {
                            inflection.setPerfect(ws);
                        }
                        break;
                    case "比较级":
                        if (ms.contains("adjective")) {
                            inflection.setComparative(ws);
                        }
                        break;
                    case "最高级":
                        if (ms.contains("adjective")) {
                            inflection.setSuperlative(ws);
                        }
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