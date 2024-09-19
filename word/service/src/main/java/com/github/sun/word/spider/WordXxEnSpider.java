package com.github.sun.word.spider;

import com.github.sun.spider.XPaths;
import com.github.sun.word.WordDictLoader;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@Service
public class WordXxEnSpider {
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
}