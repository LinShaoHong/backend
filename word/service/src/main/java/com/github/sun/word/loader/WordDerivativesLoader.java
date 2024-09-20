package com.github.sun.word.loader;

import com.github.sun.foundation.boot.Injector;
import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.word.WordDict;
import com.github.sun.word.WordDictFreq;
import com.github.sun.word.WordDictFreqMapper;
import com.github.sun.word.WordDictMapper;
import com.github.sun.word.spider.WordHcSpider;
import com.github.sun.word.spider.WordJsSpider;
import com.github.sun.word.spider.WordXdfSpider;
import com.github.sun.word.spider.WordXxEnSpider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RefreshScope
//@Service("derivatives")
public class WordDerivativesLoader extends WordBasicLoader {
    @Resource
    private WordDictMapper dictMapper;
    @Resource
    private WordLoaderAffixMapper affixMapper;

    @Override
    public void load(String word, JSON.Valuer attr, int userId) {
        retry(word, userId, dict -> {
            String root = null;
            String rootDesc = null;
            if (dict.getStruct() != null) {
                int index = -1;
                for (int i = 0; i < dict.getStruct().getParts().size(); i++) {
                    WordDict.Part part = dict.getStruct().getParts().get(i);
                    if (part.isRoot()) {
                        if (index < 0 || i == index + 1) {
                            root = root == null ? part.getPart() : root + part.getPart();
                            rootDesc = rootDesc == null ? part.getMeaningTrans() : rootDesc + "；" + part.getMeaningTrans();
                            index = i;
                        }
                    }
                }
            }
            root = StringUtils.hasText(root) ? root : word;
            String q = loadQ("cues/派生树.md");
            List<String> words = new ArrayList<>();
            words.add(word);
            words.add(root);
            WordXxEnSpider.fetchDerivative(dict.getId(), words::addAll);
            WordHcSpider.fetchDerivative(dict.getId(), root, words::addAll);
            WordJsSpider.fetchDerivative(dict.getId(), root, words::addAll);
            WordXdfSpider.fetchDerivative(dict.getId(), root, words::addAll);
            if (!Objects.equals(word, root)) {
                WordXxEnSpider.fetchDerivative(root, words::addAll);
                WordHcSpider.fetchDerivative(root, root, words::addAll);
                WordJsSpider.fetchDerivative(root, root, words::addAll);
                WordXdfSpider.fetchDerivative(root, root, words::addAll);
            }

            boolean hasRoot = dict.getStruct() != null && dict.getStruct().getParts().stream().anyMatch(WordDict.Part::isRoot);
            String resp;
            if (hasRoot) {
                List<WordLoaderAffix> affixes = affixMapper.byRoot(root);
                if (affixes.stream().map(WordLoaderAffix::getRootDesc).distinct().count() == 1L) {
                    affixes.forEach(a -> words.add(a.getId()));
                }
                q = q.replace("$input", word + "的词根为" + root + "(" + rootDesc + ")，以此直接列出它的所有同根词。注意移除含义已完全变化的单词");
                resp = assistant.chat(apiKey, model, q);
            } else {
                resp = assistant.chat(apiKey, model, q.replace("$input", "直接列出单词\"" + word + "\"的所有派生词"));
            }
            JSON.Valuer valuer = JSON.newValuer(parse(resp));
            for (JSON.Valuer v : valuer.asArray()) {
                String w = v.get("word").asText("");
                if (StringUtils.hasText(w)) {
                    words.add(w);
                }
            }
            List<String> _words = words.stream().distinct().collect(Collectors.toList());
            dict.setDerivatives(merge(dict, _words, root));
        }, "derivatives");
    }

    private List<WordDict.Derivative> merge(WordDict dict, List<String> words, String root) {
        clearInvalid(words, dict, root);
        String word = dict.getId();
        List<String> _words = words.stream().map(String::toLowerCase).distinct().collect(Collectors.toList());
        dict.getDerivatives().forEach(d -> _words.add(d.getWord()));
        List<String> ws = _words.stream().distinct().sorted(Comparator.comparingInt(String::length)).collect(Collectors.toList());
        ws.removeIf(v -> !v.toLowerCase().contains(root.toLowerCase()) || v.contains(" ") || v.contains("-") || v.contains("'"));
        List<WordDict.Derivative> list = build(word, root, ws);

        List<String> firsts = new ArrayList<>();
        list.stream().filter(v -> v.getIndex() == 1 && !Objects.equals(v.getWord(), word))
                .forEach(v -> {
                    WordXxEnSpider.fetchDerivative(v.getWord(), firsts::addAll);
                    WordHcSpider.fetchDerivative(v.getWord(), root, firsts::addAll);
                    WordJsSpider.fetchDerivative(v.getWord(), root, firsts::addAll);
                    WordXdfSpider.fetchDerivative(v.getWord(), root, firsts::addAll);
                });
        ws.addAll(firsts);

        ws.removeIf(v -> !v.toLowerCase().contains(root.toLowerCase()) || v.contains(" ") || v.contains("-") || v.contains("'"));
        words = ws.stream().distinct().sorted(Comparator.comparingInt(String::length)).collect(Collectors.toList());
        clearInvalid(words, dict, root);
        return build(word, root, words);
    }

    public static List<WordDict.Derivative> build(String word, String root, List<String> ws) {
        List<WordNode> nodes = ws.stream().map(w -> {
            WordNode node = new WordNode();
            node.setWord(w);
            if (w.contains(word) && !Objects.equals(w, word) && ws.contains(word)) {
                node.setParent(word);
            } else {
                for (int i = ws.size() - 1; i >= 0; i--) {
                    String _word = ws.get(i);
                    if (w.contains(_word) && !Objects.equals(w, _word)) {
                        node.setParent(_word);
                        break;
                    }
                }
            }
            return node;
        }).collect(Collectors.toList());

        List<WordNode> fs = nodes.stream()
                .filter(v -> Objects.equals(v.getParent(), word))
                .sorted(Comparator.comparingInt(f -> f.getWord().length()))
                .collect(Collectors.toList());
        fs.forEach(f -> {
            for (int i = fs.size() - 1; i >= 0; i--) {
                String _word = fs.get(i).getWord();
                if (f.getWord().contains(_word) && !Objects.equals(f.getWord(), _word)) {
                    f.setParent(_word);
                    break;
                }
            }
        });

        List<WordDict.Derivative> derivatives = new ArrayList<>();
        if (nodes.size() > 1) {
            Map<String, List<WordNode>> map = nodes.stream()
                    .filter(v -> v.getParent() != null)
                    .collect(Collectors.groupingBy(WordNode::getParent));

            class Util {
                public WordNode make(WordNode root, int level) {
                    List<WordNode> vs = map.getOrDefault(root.getWord(), Collections.emptyList());
                    List<WordNode> children = vs.stream()
                            .map(v -> make(v, level + 1))
                            .collect(Collectors.toList());
                    if (children.size() > 1) {
                        sort(children);
                    }
                    root.setChildren(children);
                    root.setIndex(level);
                    root.setHas(root.getWord().contains(word) || children.stream().anyMatch(WordNode::isHas));
                    return root;
                }

                public void walk(WordNode root) {
                    derivatives.add(new WordDict.Derivative(root.getWord(), root.getIndex()));
                    root.getChildren().forEach(this::walk);
                }
            }

            Util util = new Util();
            List<WordNode> trees = nodes.stream()
                    .filter(v -> v.getParent() == null)
                    .map(v -> util.make(v, 0))
                    .collect(Collectors.toList());
            sort(trees);
            trees.forEach(util::walk);
        } else if (nodes.size() == 1) {
            derivatives.add(new WordDict.Derivative(nodes.get(0).getWord(), 0));
        }
        return derivatives;
    }

    private void clearInvalid(List<String> words, WordDict dict, String root) {
        Set<String> exist = dict.getDerivatives() == null ? new HashSet<>() :
                dict.getDerivatives().stream().map(WordDict.Derivative::getWord).collect(Collectors.toSet());
        words.removeIf(v -> {
            if (Objects.equals(v, root) || Objects.equals(v, dict.getId())) {
                return false;
            }
            if (exist.contains(v)) {
                return false;
            }
            boolean invalid = !v.toLowerCase().contains(root.toLowerCase()) || v.contains(" ") || v.contains("-") || v.contains("'");
            if (invalid) {
                return true;
            }
            WordDict d = dictMapper.findById(v);
            if (d != null) {
                return false;
            }
            WordLoaderAffix a = affixMapper.findById(v);
            if (a != null) {
                return false;
            }
            return !has(v);
        });
    }

    private boolean has(String word) {
        return false;
    }

    private static void sort(List<WordNode> nodes) {
        //        List<String> all = nodes.stream().map(WordNode::getWord).collect(Collectors.toList());
        //        all = StringSorts.sort(all, 0.7);
        //        List<String> _all = all;
        //        nodes.sort(Comparator.comparingInt(v -> _all.indexOf(v.getWord())));
        //        List<String> has = nodes.stream().filter(WordNode::isHas).map(WordNode::getWord).collect(Collectors.toList());
        //        List<String> nos = nodes.stream().filter(v -> !v.isHas()).map(WordNode::getWord).collect(Collectors.toList());
        //        has.addAll(nos);
        //        nodes.sort(Comparator.comparingInt(v -> has.indexOf(v.getWord())));
        WordDictFreqMapper mapper = Injector.getInstance(WordDictFreqMapper.class);
        Set<String> ids = nodes.stream().map(v -> v.getWord().toLowerCase()).collect(Collectors.toSet());
        if (!ids.isEmpty()) {
            List<WordDictFreq> list = mapper.findByIds(ids);
            List<String> ws = list.stream().sorted(Comparator.comparingInt(WordDictFreq::getSort))
                    .map(WordDictFreq::getId).collect(Collectors.toList());
            nodes.sort(Comparator.comparingInt(v -> ws.stream().noneMatch(w -> w.equalsIgnoreCase(v.getWord())) ?
                    Integer.MAX_VALUE : ws.indexOf(v.getWord().toLowerCase())));
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WordNode {
        private String word;
        private String parent;
        private int index;
        private boolean has;
        private List<WordNode> children;
    }
}

