package com.github.sun.word.spider;

import com.github.sun.foundation.boot.utility.Strings;
import com.github.sun.spider.XPaths;
import com.github.sun.word.WordDict;
import com.github.sun.word.WordDictLoader;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@Service
public class WordJsSpider {
    public static void fetchPhonetic(WordDict dict) {
        Document node = WordDictLoader.fetchDocument("https://www.iciba.com/word?w=" + dict.getId());
        List<Node> arr = XPaths.of(node, "//ul[@class='Mean_symbols__fpCmS']/li").asArray();
        if (arr.size() > 0) {
            String uk = StringEscapeUtils.unescapeHtml4(arr.get(0).getTextContent());
            uk = StringEscapeUtils.unescapeHtml4(uk);
            dict.setUkPhonetic("/ " + parsePhonetic(uk) + " /");
        }
        if (arr.size() > 1) {
            String us = StringEscapeUtils.unescapeHtml4(arr.get(1).getTextContent());
            us = StringEscapeUtils.unescapeHtml4(us);
            dict.setUkPhonetic("/ " + parsePhonetic(us) + " /");
        }
    }

    private static String parsePhonetic(String s) {
        Strings.Parser parser = Strings.newParser().set(s);
        parser.next(Pattern.compile("[^\\[]*"));
        parser.next(Pattern.compile("\\["));
        return parser.next(Pattern.compile("[^]]*"));
    }

    @SuppressWarnings("Duplicates")
    public static void fetchInflection(WordDict dict) {
        try {
            Document node = WordDictLoader.fetchDocument("https://www.iciba.com/word?w=" + dict.getId());
            WordDict.Inflection inflection = dict.getInflection();
            if (inflection == null) {
                inflection = new WordDict.Inflection();
            }
            List<Node> arr = XPaths.of(node, "//ul[@class='Morphology_morphology__vNvkI']/li").asArray();
            for (Node v : arr) {
                String words = XPaths.of(v, "./span").asText();
                Strings.Parser parser = Strings.newParser().set(v.getTextContent());
                Pattern pattern = Pattern.compile("[\\u4E00-\\u9FFF]");
                while (parser.skip(pattern)) {
                    parser.skip(pattern);
                }
                String name = parser.left();
                switch (name) {
                    case "复数":
                        inflection.setPlural(Collections.singletonList(words));
                        break;
                    case "第三人称单数":
                        inflection.setThirdPresent(Collections.singletonList(words));
                        break;
                    case "现在分词":
                        inflection.setProgressive(Collections.singletonList(words));
                        break;
                    case "过去式":
                        inflection.setPast(Collections.singletonList(words));
                        break;
                    case "过去分词":
                        inflection.setPerfect(Collections.singletonList(words));
                        break;
                    case "比较级":
                        inflection.setComparative(Collections.singletonList(words));
                        break;
                    case "最高级":
                        inflection.setSuperlative(Collections.singletonList(words));
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

    @SuppressWarnings("Duplicates")
    public static Set<String> fetchMeaning(WordDict dict) {
        Set<String> set = new LinkedHashSet<>();
        try {
            Document node = WordDictLoader.fetchDocument("https://www.iciba.com/word?w=" + dict.getId());
            List<Node> arr = XPaths.of(node, "//ul[@class='Mean_part__UI9M6']/li").asArray();
            arr.forEach(v -> {
                String text = XPaths.of(v, "./i").asText();
                if ("n.".equals(text)) {
                    set.add("nouns");
                }
                if ("v.".equals(text)) {
                    set.add("verbs");
                }
                if ("adj.".equals(text)) {
                    set.add("adjectives");
                }
                if ("adv.".equals(text)) {
                    set.add("adverbs");
                }
            });
        } catch (Exception ex) {
            //do nothing
        }
        return set;
    }

    @SuppressWarnings("Duplicates")
    public static String fetchRoot(WordDict dict) {
        String root = null;
        try {
            Document node = WordDictLoader.fetchDocument("https://www.iciba.com/word?w=" + dict.getId());
            String name = XPaths.of(node, "//div[@class='Affix_affix__iiL_9']/p").as().getTextContent();
            if (name.contains("词根")) {
                Strings.Parser parser = Strings.newParser().set(name);
                parser.next(Pattern.compile("[^a-z]*"));
                root = parser.right();
            }
        } catch (Exception ex) {
            //do nothing
        }
        return root;
    }

    @SuppressWarnings("Duplicates")
    public static void fetchDerivative(String w, String root, Consumer<Set<String>> func) {
        try {
            Set<String> words = new LinkedHashSet<>();
            Document node = WordDictLoader.fetchDocument("https://www.iciba.com/word?w=" + w);
            Node rootNode = XPaths.of(node, "//div[@class='Affix_affix__iiL_9']").as();
            if (rootNode != null) {
                String desc = XPaths.of(rootNode, "./p[2]").as().getTextContent();
                desc = StringEscapeUtils.unescapeHtml4(desc);

                if (StringUtils.hasText(desc) && !desc.contains("1.") && !desc.contains("2.")) {
                    List<Node> arr = XPaths.of(rootNode, "./ul/li/div/h5").asArray();
                    arr.forEach(a -> {
                        String name = a.getTextContent();
                        Strings.Parser parser = Strings.newParser().set(name);
                        parser.next(Pattern.compile("[a-z]*"));
                        String word = parser.left();
                        if (StringUtils.hasText(word) && word.contains(root)) {
                            words.add(word);
                        }
                    });
                }
            }
            func.accept(words);
        } catch (Throwable ex) {
            //do nothing
        }
    }

    public static Set<String> fetchDiffs(WordDict dict) {
        Set<String> set = new HashSet<>();
        try {
            Document node = WordDictLoader.fetchDocument("https://www.iciba.com/word?w=" + dict.getId());
            List<Node> arr = XPaths.of(node, "//div[@class='SameAnalysis_sameAnalysis__fkwr8'][1]//span[@class='jsx-1012413381']").asArray();
            arr.forEach(a -> set.add(a.getTextContent()));
        } catch (Exception ex) {
            //do nothing
        }
        return set;
    }
}