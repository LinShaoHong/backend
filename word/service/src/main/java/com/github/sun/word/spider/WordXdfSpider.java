package com.github.sun.word.spider;

import com.github.sun.spider.XPaths;
import com.github.sun.word.WordDictLoader;
import com.github.sun.word.loader.WordLoaderBook;
import com.github.sun.word.loader.WordLoaderBookMapper;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Consumer;

@Service
public class WordXdfSpider {
    @Resource
    private WordLoaderBookMapper mapper;

    public static void fetchDerivative(String word, String root, Consumer<Set<String>> func) {
        try {
            Set<String> words = new LinkedHashSet<>();
            Document node = WordDictLoader.fetchDocument("https://www.koolearn.com/dict/search/index?keywords=" + word);
            List<Node> arr = XPaths.of(node, "//div[@class='retrieve']/div").asArray();
            Arrays.asList("同根词", "同义词", "反义词").forEach(t -> {
                int j = -1;
                for (int i = 0; i < arr.size(); i++) {
                    if (arr.get(i).getTextContent().contains(t)) {
                        j = i + 1;
                        break;
                    }
                }
                if (j > 0) {
                    Node div = arr.get(j);
                    XPaths.of(div, "./a").asArray().forEach(a -> {
                        String name = StringEscapeUtils.unescapeHtml4(a.getTextContent()).trim();
                        if (name.split(" ").length == 1) {
                            if ("同根词".equals(t) || name.contains(root)) {
                                words.add(name);
                            }
                        }
                    });
                }
            });
            func.accept(words);
        } catch (Throwable ex) {
            // do nothing
        }
    }

    @SuppressWarnings("Duplicates")
    public void fetchWords(String uri,
                           String category,
                           String tag,
                           int start,
                           int end) {
        for (int i = start; i <= end; i++) {
            String url = String.format(uri, i);
            Document node = WordDictLoader.fetchDocument(url);
            List<Node> arr = XPaths.of(node, "//a[@class='word']").asArray();
            arr.forEach(a -> {
                String word = a.getTextContent();
                WordLoaderBook book = mapper.findById(word);
                List<WordLoaderBook.Scope> scopes = book == null ? new ArrayList<>() : book.getScopes();
                WordLoaderBook.Scope scope = scopes.stream()
                        .filter(s -> Objects.equals(s.getCategory(), category))
                        .findFirst().orElse(null);
                if (scope == null) {
                    scope = new WordLoaderBook.Scope();
                    scope.setCategory(category);
                    scope.setTags(new HashSet<>());
                    scopes.add(scope);
                }
                scope.getTags().add(tag);

                book = book == null ? new WordLoaderBook() : book;
                book.setScopes(scopes);
                if (book.getId() == null) {
                    book.setId(word);
                    mapper.insert(book);
                } else {
                    mapper.update(book);
                }
            });
        }
    }
}