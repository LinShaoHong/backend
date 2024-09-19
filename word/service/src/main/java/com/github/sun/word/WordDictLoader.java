package com.github.sun.word;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.sun.foundation.ai.Assistant;
import com.github.sun.foundation.boot.Scanner;
import com.github.sun.foundation.boot.exception.ConstraintException;
import com.github.sun.foundation.boot.utility.Dates;
import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.foundation.boot.utility.Reflections;
import com.github.sun.foundation.sql.IdGenerator;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.spider.Fetcher;
import com.github.sun.spider.XPaths;
import com.github.sun.word.loader.*;
import com.github.sun.word.spider.WordHcSpider;
import com.github.sun.word.spider.WordJsSpider;
import com.github.sun.word.spider.WordXdfSpider;
import com.github.sun.word.spider.WordXxEnSpider;
import com.ibm.icu.impl.data.ResourceReader;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;

import javax.annotation.Resource;
import javax.ws.rs.client.Client;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RefreshScope
public class WordDictLoader {
    private final static ClassLoader loader = ResourceReader.class.getClassLoader();
    private static final HtmlCleaner hc = new HtmlCleaner();
    private final static ExecutorService executor = Executors.newFixedThreadPool(10);

    @Value("${qwen.key}")
    private String apiKey;
    @Value("${qwen.model}")
    private String model;
    @Resource(name = "qwen")
    private Assistant assistant;
    @Resource(name = "mysql")
    protected SqlBuilder.Factory factory;
    @Resource
    private Client client;
    @Resource
    private WordDictMapper mapper;
    @Resource
    private WordDictDiffMapper diffMapper;
    @Resource
    private WordLoaderCheckMapper checkMapper;
    @Resource
    private WordLoaderCodeMapper codeMapper;
    @Resource
    private WordLoaderAffixMapper affixMapper;
    @Resource
    private WordDictTreeMapper treeMapper;
    @Resource
    private WordLoaderBookMapper tagMapper;
    @Resource
    private WordDictLemmaMapper lemmaMapper;
    @Resource
    private WordLoaderEcMapper ecMapper;
    @Resource
    private WordDictFreqMapper freqMapper;
    @Resource
    private WordLoaderBookMapper bookMapper;

    public String chat(String q) {
        return assistant.chat(apiKey, model, q);
    }

    public void fetch(int userId) {
        SqlBuilder sb = factory.create();
        SqlBuilder.Template template = sb.from(WordLoaderBook.class).limit(300, 500).template();
        tagMapper.findByTemplate(template).forEach(tag -> {
            loadAll(tag.getId(), userId);
        });
    }

    public void loadAll(String words, int userId) {
        for (String word : words.split(",")) {
            WordBasicLoader.init(word, userId);
            Scanner.getClassesWithInterface(WordLoader.class)
                    .stream()
                    .filter(Scanner.ClassTag::isImplementClass)
                    .filter(v -> v.runtimeClass() != WordDerivativesLoader.class)
                    .forEach(loader -> executor.submit(() -> loader.getInstance().load(word, userId)));
        }
    }

    public void loadPart(String word, String part, JsonNode attr, int userId) {
        WordBasicLoader.init(word, userId);
        mapper.loading(word, "'$." + part + "Loading'");
        Scanner.getClassesWithInterface(WordLoader.class)
                .stream().filter(v -> v.isImplementClass() && v.runtimeClass().isAnnotationPresent(Service.class) &&
                        v.runtimeClass().getAnnotation(Service.class).value().equals(part))
                .findFirst()
                .ifPresent(loader -> executor.submit(() -> loader.getInstance().load(word, attr == null ? null : JSON.newValuer(attr), userId)));
    }

    @Transactional
    public void removePart(String word, String part, String path, JsonNode attr, int userId) {
        WordDict dict = WordBasicLoader.init(word, userId);
        switch (part) {
            case "phonetic":
                if ("uk".equals(path)) {
                    dict.setUkPhonetic(null);
                    dict.setUkAudioId(null);
                } else {
                    dict.setUsPhonetic(null);
                    dict.setUsAudioId(null);
                }
                mapper.update(dict);
                break;
            case "meaning":
                if (StringUtils.hasText(path)) {
                    Reflections.setValue(dict.getMeaning(), path, "");
                } else {
                    dict.setMeaning(new WordDict.TranslatedMeaning());
                }
                dict.setPassed(false);
                mapper.update(dict);
                break;
            case "struct":
                if (!StringUtils.hasText(path)) {
                    dict.setStruct(null);
                }
                dict.setPassed(false);
                mapper.update(dict);
                break;
            case "inflection":
                if (StringUtils.hasText(path)) {
                    Reflections.setValue(dict.getInflection(), path, Collections.emptyList());
                } else {
                    dict.setInflection(new WordDict.Inflection());
                }
                dict.setPassed(false);
                mapper.update(dict);
                break;
            case "formula":
                if (dict.getCollocation() != null) {
                    dict.getCollocation().setFormulas(new ArrayList<>());
                    dict.setPassed(false);
                    mapper.update(dict);
                }
                break;
            case "collocation":
                if (StringUtils.hasText(path)) {
                    String removed = path.endsWith(":phrase") ? path.substring(0, path.length() - 7) : path;
                    if (path.endsWith(":phrase")) {
                        dict.getCollocation().getPhrases().removeIf(d -> Objects.equals(d.getEn(), removed));
                    } else {
                        dict.getCollocation().getFormulas().removeIf(d -> Objects.equals(d.getEn(), removed));
                    }
                } else {
                    dict.setCollocation(null);
                }
                dict.setPassed(false);
                mapper.update(dict);
                break;
            case "derivatives":
                if (StringUtils.hasText(path)) {
                    String removed = path.endsWith(":sub") ? path.substring(0, path.length() - 4) : path;
                    String treeId = attr.get("treeId").asText();
                    int version = attr.get("version").asInt();
                    WordDictTree tree = treeMapper.findById(treeId);
                    List<WordDictTree.Derivative> vs = tree.getDerivatives();
                    int j = -1;
                    for (int i = 0; i < vs.size(); i++) {
                        if (vs.get(i).getWord().equals(removed)) {
                            j = i;
                            break;
                        }
                    }
                    if (j >= 0) {
                        int z = j;
                        for (int k = j + 1; k < vs.size(); k++) {
                            if (vs.get(k).getIndex() <= vs.get(j).getIndex()) {
                                break;
                            }
                            z = k;
                        }
                        if (path.endsWith(":sub")) {
                            List<WordDictTree.Derivative> ds = new ArrayList<>();
                            for (int i = j; i <= z; i++) {
                                ds.add(vs.get(i));
                            }
                            vs.removeAll(ds);
                        } else {
                            for (int i = j + 1; i <= z; i++) {
                                vs.get(i).setIndex(vs.get(i).getIndex() - 1);
                            }
                            vs.remove(j);
                        }
                    }
                    editTree(tree.getRoot(), tree.getRootDesc(), version, vs);
                }
                break;
            case "differs":
                if (StringUtils.hasText(path)) {
                    diffMapper.deleteById(path);
                }
                mapper.noPass(word);
                break;
            case "synonym":
                if (StringUtils.hasText(path)) {
                    dict.getSynAnts().getSynonyms().removeIf(d -> Objects.equals(d, path));
                } else {
                    dict.getSynAnts().setSynonyms(new ArrayList<>());
                }
                dict.setPassed(false);
                mapper.update(dict);
                break;
            case "antonym":
                if (StringUtils.hasText(path)) {
                    dict.getSynAnts().getAntonyms().removeIf(d -> Objects.equals(d, path));
                } else {
                    dict.getSynAnts().setAntonyms(new ArrayList<>());
                }
                dict.setPassed(false);
                mapper.update(dict);
                break;
            case "tree":
                treeMapper.deleteById(path);
                break;
            default:
                break;
        }
    }

    @Transactional
    public int remove(String word) {
        int next = 1;
        WordDict dict = mapper.findById(word);
        if (dict != null) {
            String date = Dates.format(dict.getLoadTime());
            mapper.dec(dict.getSort(), date);
            mapper.deleteById(word);
            next = dict.getSort();
            int total = mapper.countByLoadTime(date);
            if (total < next) {
                next = total;
            }
            codeMapper.updateById(date + ":1", total);
        }
        return next;
    }

    @Transactional
    public void pass(String word) {
        mapper.pass(word);
    }

    @Transactional
    public WordLoaderCheck stat(String date, int userId) {
        WordLoaderCheck check = checkMapper.findById(date + ":" + userId);
        check.setTotal(mapper.countByLoadTime(date));
        check.setPassed(mapper.countByPassed(date));
        check.setViewed(mapper.countByViewed(date));
        return check;
    }

    @Transactional
    public List<WordLoaderCheck> stats(int userId) {
        List<String> dates = checkMapper.dates();
        return dates.stream().map(date -> {
            String id = date + ":" + userId;
            WordLoaderCheck check = checkMapper.findById(id);
            if (check == null) {
                check = new WordLoaderCheck();
                check.setId(date + ":" + userId);
                check.setUserId(userId);
                check.setDate(date);
                check.setSort(1);
                checkMapper.replace(check);
            }
            check.setTotal(mapper.countByLoadTime(date));
            check.setPassed(mapper.countByPassed(date));
            check.setViewed(mapper.countByViewed(date));
            return check;
        }).collect(Collectors.toList());
    }

    @Transactional
    public WordDict dict(String date, Integer sort, int userId) {
        WordLoaderCheck check = null;
        List<WordLoaderCheck> all = stats(userId);
        if (!StringUtils.hasText(date)) {
            check = all.stream().filter(WordLoaderCheck::isCurr).findFirst().orElse(null);
            if (check == null) {
                check = all.get(all.size() - 1);
            }
            date = check.getDate();
        }
        check = check == null ? checkMapper.findById(date + ":" + userId) : check;
        if (sort == null) {
            sort = check.getSort();
        }
        WordDict dict = mapper.byDateAndSort(date, sort);
        if (dict != null) {
            check.setSort(sort);
            check.setCurr(true);
            checkMapper.update(check);
            checkMapper.past(date + ":" + userId, userId);

            mapper.viewed(dict.getId());
        }
        return dict;
    }

    public List<WordDict> dicts(String date) {
        return mapper.byDate(date);
    }

    public Set<Root> roots(String root) {
        Set<Root> roots = new LinkedHashSet<>();
        for (String ro : root.split(",")) {
            List<WordLoaderAffix> list = affixMapper.byRoot(ro);
            Set<String> desc = list.stream().map(WordLoaderAffix::getRootDesc)
                    .filter(StringUtils::hasText).collect(Collectors.toSet());
            if (!desc.isEmpty()) {
                list.addAll(affixMapper.byRootDesc(desc));
            }
            list.forEach(v -> {
                Root _root = roots.stream()
                        .filter(r -> r.getDesc().equals(v.getRootDesc()))
                        .findFirst().orElse(null);
                if (_root == null) {
                    _root = new Root();
                    _root.setRoots(new LinkedHashSet<>());
                    _root.setDesc(v.getRootDesc());
                    roots.add(_root);
                }
                _root.getRoots().add(v.getRoot());
            });
        }
        return roots;
    }

    @Data
    public static class Root {
        private Set<String> roots;
        private String desc;
    }

    public WordLoaderAffix affix(String word) {
        return affixMapper.byId(word);
    }

    public static synchronized Document fetchDocument(String url) {
        return fetchDocument(url, null);
    }

    public static synchronized Document fetchDocument(String url, Map<String, String> headers) {
        try {
            String html = Fetcher.builder()
                    .uri(url)
                    .headers(headers)
                    .fetch();
            return new DomSerializer(new CleanerProperties()).createDOM(hc.clean(html));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void editMeaning(String id, WordDict.TranslatedMeaning meaning) {
        SqlBuilder sb = factory.create();
        SqlBuilder.Template template = sb.from(WordDict.class)
                .where(sb.field("id").eq(id))
                .update()
                .set("passed", 0)
                .set("meaning", meaning)
                .template();
        mapper.updateByTemplate(template);
    }

    public void editStruct(String id, WordDict.Struct struct) {
        struct.getParts().removeIf(v -> !StringUtils.hasText(v.getPart()));
        SqlBuilder sb = factory.create();
        SqlBuilder.Template template = sb.from(WordDict.class)
                .where(sb.field("id").eq(id))
                .update()
                .set("passed", 0)
                .set("struct", struct)
                .template();
        mapper.updateByTemplate(template);
    }

    public WordDictTree moveDerivative(String id, int version, String word, String op) {
        WordDictTree tree = treeMapper.findById(id);
        List<WordDictTree.Derivative> list = tree.getDerivatives();
        List<WordDictTree.Derivative> derivatives = new ArrayList<>();
        class Util {
            public List<Node> make(String parent, int pos, int index) {
                List<Node> cs = new ArrayList<>();
                for (int i = pos; i < list.size(); i++) {
                    WordDictTree.Derivative v = list.get(i);
                    if (v.getIndex() < index) {
                        break;
                    }
                    if (v.getIndex() == index) {
                        Node node = new Node();
                        node.setWord(v.getWord());
                        node.setVersion(v.getVersion());
                        node.setMerged(v.isMerged());
                        node.setParent(parent);
                        node.setChildren(make(node.getWord(), i + 1, v.getIndex() + 1));
                        cs.add(node);
                    }
                }
                return cs;
            }

            public void walk(Node node, int index) {
                derivatives.add(new WordDictTree.Derivative(node.getWord(), index, node.getVersion(), node.isMerged()));
                node.getChildren().forEach(c -> walk(c, index + 1));
            }

            public Node find(List<Node> ns, String word) {
                for (Node n : ns) {
                    if (Objects.equals(n.getWord(), word)) {
                        return n;
                    } else {
                        List<Node> cs = n.getChildren();
                        Node c = find(cs, word);
                        if (c != null) {
                            return c;
                        }
                    }
                }
                return null;
            }
        }
        Util util = new Util();
        List<Node> nodes = util.make(null, 0, 0);
        Node curr = util.find(nodes, word);
        if (curr != null) {
            Node parent = StringUtils.hasText(curr.getParent()) ?
                    util.find(nodes, curr.getParent()) : null;
            Node grandParent = null;
            if (parent != null) {
                grandParent = StringUtils.hasText(parent.getParent()) ?
                        util.find(nodes, parent.getParent()) : null;
            }
            int i;
            List<Node> brs;
            switch (op) {
                case "left":
                    if (parent != null) {
                        brs = grandParent != null ? grandParent.getChildren() : nodes;
                        parent.getChildren().remove(curr);
                        i = brs.indexOf(parent);
                        brs.add(i + 1, curr);
                    }
                    break;
                case "right":
                    brs = parent == null ? nodes : parent.getChildren();
                    i = brs.indexOf(curr);
                    if (i > 0) {
                        brs.remove(curr);
                        brs.get(i - 1).getChildren().add(curr);
                    }
                    break;
                case "up":
                    brs = parent == null ? nodes : parent.getChildren();
                    i = brs.indexOf(curr);
                    if (i > 0) {
                        Collections.swap(brs, i - 1, i);
                    }
                    break;
                case "down":
                    brs = parent == null ? nodes : parent.getChildren();
                    i = brs.indexOf(curr);
                    if (i < brs.size() - 1) {
                        Collections.swap(brs, i, i + 1);
                    }
                    break;
                default:
                    break;
            }
        }
        nodes.forEach(n -> util.walk(n, 0));
        return editTree(tree.getRoot(), tree.getRootDesc(), version, derivatives);
    }

    public WordDictTree addDerivative(String id, String word, String input, int version) {
        WordDictTree tree = treeMapper.findById(id);
        List<WordDictTree.Derivative> list = tree.getDerivatives();
        if (list.stream().noneMatch(v -> Objects.equals(v.getWord(), input))) {
            if (!StringUtils.hasText(word)) {
                tree.getDerivatives().add(new WordDictTree.Derivative(input, 0, tree.getVersion() + 1, true));
            } else {
                int j = -1;
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).getWord().equals(word)) {
                        j = i;
                    }
                }
                if (j >= 0) {
                    int z = j;
                    for (int k = j + 1; k < list.size(); k++) {
                        if (list.get(k).getIndex() <= list.get(j).getIndex()) {
                            break;
                        }
                        z = k;
                    }
                    tree.getDerivatives().add(z + 1, new WordDictTree.Derivative(input, list.get(j).getIndex() + 1, tree.getVersion() + 1, true));
                }
            }
            return editTree(tree.getRoot(), tree.getRootDesc(), version, tree.getDerivatives());
        }
        return null;
    }

    public List<WordDict> search(String q) {
        SqlBuilder sb = factory.create();
        SqlBuilder.Template template = sb.from(WordDict.class)
                .where(sb.field("id").contains(q))
                .select("id", "meaning", "loadState", "passed", "viewed", "sort", "loadTime")
                .template();
        return mapper.findByTemplate(template);
    }

    public List<Differs> differs(String word) {
        List<WordDictDiff> list = diffMapper.byWord(word);
        return list.stream().map(v -> {
            Differs differs = new Differs();
            differs.setMeaning(v.getMean());
            List<String> ws = v.getWords();
            int i = ws.indexOf(word);
            if (i > 0) {
                Collections.swap(ws, 0, i);
            }
            List<WordDictDiff> words = diffMapper.byDiffId(v.getDiffId());
            words.sort(Comparator.comparingInt(w -> ws.indexOf(w.getWord())));
            differs.setWords(words);
            return differs;
        }).sorted(Comparator.comparingInt(v -> v.getWords().size())).collect(Collectors.toList());
    }

    @Data
    public static class Differs {
        private String meaning;
        private List<WordDictDiff> words;
    }

    public List<WordDictTree> trees(String root) {
        return treeMapper.byRoot(root);
    }

    public List<WordDictTree> findTree(String word) {
        List<WordDictTree> list = treeMapper.findTree("{\"word\":\"" + word + "\"}");
        if (list.isEmpty()) {
            WordDict dict = mapper.findById(word);
            if (dict != null && dict.getStruct() != null && dict.getStruct().getParts() != null) {
                return dict.getStruct().getParts().stream()
                        .filter(WordDict.Part::isRoot)
                        .flatMap(p ->
                                treeMapper.findTree("{\"word\":\"" + p.getPart() + "\"}").stream())
                        .collect(Collectors.toList());
            }
        }
        return list;
    }

    public void createTree(String word) {
        WordDict dict = mapper.findById(word);
        if (dict != null) {
            if (dict.getLoadState().isCreateTreeLoading()) {
                return;
            }
            try {
                mapper.loading(word, "'$.createTreeLoading'");
                dict.getStruct().getParts().stream().filter(WordDict.Part::isRoot).forEach(part -> {
                    String root = part.getPart();
                    String desc = part.getMeaningTrans();
                    WordDictTree tree = treeMapper.byRootAndDesc(root, desc);
                    if (tree == null) {
                        List<String> ws = fetchDerivatives(root, word, root);
                        ws = ws.stream().distinct().sorted(Comparator.comparingInt(String::length)).collect(Collectors.toList());
                        List<WordDictTree.Derivative> derivatives = WordDerivativesLoader.build(word, root, ws).stream()
                                .map(v -> new WordDictTree.Derivative(v.getWord(), v.getIndex(), 1, false))
                                .collect(Collectors.toList());
                        editTree(root, desc, 0, derivatives);
                    }
                });
            } finally {
                mapper.loaded(word, "'$.createTreeLoading'");
            }
        }
    }

    public WordDictTree mergeTree(String treeId, String word) {
        WordDict dict = mapper.findById(word);
        if (dict != null) {
            if (dict.getLoadState().isMergeTreeLoading()) {
                return null;
            }
            try {
                mapper.loading(word, "'$.mergeTreeLoading'");
                WordDictTree tree = treeMapper.findById(treeId);
                String root = tree.getRoot();
                List<WordDictTree.Derivative> derivatives = tree.getDerivatives();
                List<String> ws = fetchDerivatives(word, word);
                ws.removeIf(v -> derivatives.stream().anyMatch(d -> Objects.equals(d.getWord(), v)) || root.contains(v));
                if (ws.isEmpty()) {
                    return tree;
                }
                ws.add(root);
                ws.add(word);
                ws = ws.stream().distinct().sorted(Comparator.comparingInt(String::length)).collect(Collectors.toList());
                List<WordDict.Derivative> news = WordDerivativesLoader.build(word, root, ws);
                List<WordDictTree.Derivative> ds = tree.getDerivatives();
                for (int i = news.size() - 1; i >= 0; i--) {
                    WordDict.Derivative n = news.get(i);
                    if (derivatives.stream().anyMatch(d -> Objects.equals(d.getWord(), n.getWord()))) {
                        continue;
                    }
                    if (n.getIndex() == 0) {
                        if (!n.getWord().equalsIgnoreCase(root)) {
                            ds.add(1, new WordDictTree.Derivative(n.getWord(), n.getWord().contains(root) ? 0 : 1, tree.getVersion() + 1, true));
                        }
                    } else {
                        ds.add(1, new WordDictTree.Derivative(n.getWord(), n.getWord().contains(root) ? n.getIndex() : n.getIndex() + 1, tree.getVersion() + 1, true));
                    }
                }
                int max = 0;
                for (WordDictTree.Derivative d : ds) {
                    if (d.getIndex() > max + 1) {
                        d.setIndex(max + 1);
                    } else {
                        max = d.getIndex();
                    }
                }
                return editTree(tree.getRoot(), tree.getRootDesc(), tree.getVersion(), ds);
            } finally {
                mapper.loaded(word, "'$.mergeTreeLoading'");
            }
        }
        return null;
    }

    public void editTreeDesc(String treeId, String desc, int version) {
        SqlBuilder sb = factory.create();
        SqlBuilder.Template template = sb.from(WordDictTree.class)
                .where(sb.field("id").eq(treeId))
                .forUpdate()
                .template();
        WordDictTree tree = treeMapper.findOneByTemplate(template);
        if (tree.getVersion() != version) {
            throw new ConstraintException("");
        }
        tree.setRootDesc(desc);
        treeMapper.update(tree);
    }

    private WordDictTree editTree(String root,
                                  String desc,
                                  int version,
                                  List<WordDictTree.Derivative> derivatives) {
        WordDictTree tree = treeMapper.byRootAndDesc(root, desc);
        if (tree != null) {
            if (version != tree.getVersion()) {
                return tree;
            }
            SqlBuilder sb = factory.create();
            SqlBuilder.Template template = sb.from(WordDictTree.class)
                    .where(sb.field("id").eq(tree.getId()))
                    .forUpdate()
                    .template();
            tree = treeMapper.findOneByTemplate(template);
            tree.setDerivatives(derivatives);
            tree.setVersion(tree.getVersion() + 1);
            treeMapper.update(tree);
            return tree;
        } else {
            tree = new WordDictTree();
            tree.setId(IdGenerator.next());
            tree.setRoot(root);
            tree.setRootDesc(desc);
            tree.setVersion(1);
            tree.setDerivatives(derivatives);
            treeMapper.insert(tree);
        }
        return tree;
    }

    @SuppressWarnings("Duplicates")
    private List<String> fetchDerivatives(String root, String... words) {
        List<String> ws = new ArrayList<>();
        Arrays.asList(words).forEach(w -> {
            ws.add(w);
            WordXxEnSpider.fetchDerivative(w, ws::addAll);
            WordHcSpider.fetchDerivative(w, root, ws::addAll);
            WordJsSpider.fetchDerivative(w, root, ws::addAll);
            WordXdfSpider.fetchDerivative(w, root, ws::addAll);
        });
        List<String> _ws = ws.stream()
                .flatMap(v -> Arrays.stream(v.split("/")))
                .filter(v -> !v.contains(" ") && !v.contains("-") && !v.contains("'") && v.contains(root))
                .distinct()
                .collect(Collectors.toList());
        _ws.forEach(w -> {
            if (!Arrays.asList(words).contains(w)) {
                WordXxEnSpider.fetchDerivative(w, ws::addAll);
                WordHcSpider.fetchDerivative(w, root, ws::addAll);
                WordJsSpider.fetchDerivative(w, root, ws::addAll);
                WordXdfSpider.fetchDerivative(w, root, ws::addAll);
            }
        });
        List<String> ret = ws.stream()
                .flatMap(v -> Arrays.stream(v.split("/")))
                .filter(v -> !v.contains(" ") && !v.contains("-") && !v.contains("'") && v.contains(root))
                .distinct()
                .collect(Collectors.toList());
        ret.removeIf(v -> !StringUtils.hasText(v.trim()));
        //去除无效
        ret = ret.stream().map(v -> {
            if (!v.contains(root)) {
                return null;
            }
            if (Arrays.asList(words).contains(v)) {
                return v;
            }
            boolean invalid = v.contains(" ") || v.contains("-") || v.contains("'");
            if (invalid) {
                return null;
            }
            WordDict d = mapper.findById(v);
            if (d != null && Objects.equals(d.getId(), v)) {
                return v;
            }
            WordLoaderAffix a = affixMapper.findById(v);
            if (a != null && Objects.equals(a.getId(), v)) {
                return v;
            }
            return has(v);
        }).filter(Objects::nonNull).collect(Collectors.toList());

        //去除lemmas
        Function<Set<String>, Set<String>> lemmasFunc = arg -> {
            Set<String> set = new HashSet<>();
            arg.forEach(a -> lemmaMapper.byInf(a).forEach(v -> set.add(v.getId())));
            return set;
        };
        Set<String> list = lemmasFunc.apply(new HashSet<>(ret));
        while (!list.isEmpty()) {
            for (String v : list) {
                if (!root.contains(v)) {
                    String h = has(v);
                    if (h != null) {
                        ret.add(h);
                    }
                }
            }
            Set<String> tmp = new HashSet<>(list);
            list = lemmasFunc.apply(list);
            if (list.equals(tmp)) {
                break;
            }
        }

        List<WordDictLemma> lemmas = lemmaMapper.findByIds(new HashSet<>(ret))
                .stream().filter(WordDictLemma::isHas).collect(Collectors.toList());
        Set<String> vis = lemmas.stream().flatMap(v -> v.getInflections().stream())
                .map(this::has).filter(Objects::nonNull).collect(Collectors.toSet());
        ret.addAll(vis);
        vis.removeIf(vi -> {
            WordLoaderEc ec = ecMapper.findById(vi);
            if (ec != null) {
                String trans = ec.getTranslation();
                if (StringUtils.hasText(trans)) {
                    return Arrays.stream(trans.split("\n")).anyMatch(s -> s.startsWith("a."));
                }
                return true;
            } else {
                try {
                    Document node = WordDictLoader.fetchDocument("https://www.merriam-webster.com/dictionary/" + vi);
                    List<org.w3c.dom.Node> arr = XPaths.of(node, "//div[@class='entry-word-section-container']").asArray();
                    return arr.stream().anyMatch(a -> {
                        String w = XPaths.of(a, ".//h1[@class='hword']").asText();
                        String speech = XPaths.of(a, ".//h2[@class='parts-of-speech']/a").asText();
                        return vi.equalsIgnoreCase(w) && "adjective".equalsIgnoreCase(speech);
                    });
                } catch (Throwable ex) {
                    return false;
                }
            }
        });
        ret.removeIf(v -> {
            if (Arrays.asList(words).contains(v)) {
                return false;
            }
            return !v.contains(root) || vis.stream().anyMatch(s -> s.equalsIgnoreCase(v)) || v.contains("-") || v.contains(" ");
        });

        List<String> _ret = ret;
        if (!_ret.isEmpty()) {
            List<WordLoaderAffix> affixes = affixMapper.findByIds(new HashSet<>(_ret));
            affixes.stream()
                    .filter(a -> Objects.equals(a.getRoot(), root) && StringUtils.hasText(a.getRootDesc()))
                    .findFirst()
                    .ifPresent(a -> {
                        affixMapper.byRootDesc(Collections.singleton(a.getRootDesc())).forEach(aa -> _ret.add(aa.getId()));
                    });
        }
        return _ret;
    }

    private String has(String word) {
        String w = word;
        WordDictFreq freq = freqMapper.findById(w);
        boolean has = freq != null;
        if (has) {
            WordLoaderEc ec = ecMapper.findById(w);
            if (ec != null) {
                w = ec.getId();
            } else {
                try {
                    Document node = WordDictLoader.fetchDocument("https://www.merriam-webster.com/dictionary/" + w);
                    List<org.w3c.dom.Node> arr = XPaths.of(node, "//h1[@class='hword']").asArray();
                    if (!arr.isEmpty()) {
                        w = arr.get(0).getTextContent();
                    }
                } catch (Throwable ex) {
                    //do nothing
                }
            }
        }
        return has ? w : null;
    }

    public void loadBooks(String dir) {
        Map<String, String> map = new LinkedHashMap<>() {{
            put("cz:zk", "中考词汇");
            put("gz:gk", "高考词汇");
            put("ky:ky", "考研词汇");
            put("un:cet4", "CET-4四级词汇");
            put("un:cet6", "CET-6六级词汇");
            put("un:level4", "专业四级词汇");
            put("un:level8", "专业八级词汇");
            put("cg:sat", "SAT词汇");
            put("cg:gre", "GRE词汇");
            put("cg:gmat", "GMAT词汇");
            put("cg:ielts", "雅思词汇");
            put("cg:toefl", "TOEFL托福词汇");
        }};
        map.forEach((k, v) -> {
            try {
                System.out.println(v);
                String[] arr = k.split(":");
                Files.walkFileTree(Paths.get(dir + arr[1]), new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.lines(file).forEach(line -> _loadBook(arr[0], v, line));
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    public void loadBook(String path, String tag, String name) {
        try (InputStream in = loader.getResourceAsStream(path + ".json")) {
            assert in != null;
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line = reader.readLine();
            while (StringUtils.hasText(line)) {
                _loadBook(tag, name, line);
                line = reader.readLine();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void _loadBook(String tag, String name, String line) {
        String word = JSON.asJsonNode(line).get("headWord").asText();
        WordLoaderBook book = bookMapper.findById(word);
        List<WordLoaderBook.Scope> scopes = book == null ? new ArrayList<>() : book.getScopes();
        WordLoaderBook.Scope scope = scopes.stream()
                .filter(s -> Objects.equals(s.getTag(), tag))
                .findFirst().orElse(null);
        if (scope == null) {
            scope = new WordLoaderBook.Scope();
            scope.setTag(tag);
            scope.setNames(new HashSet<>());
            scopes.add(scope);
        }
        scope.getNames().add(name);

        book = book == null ? new WordLoaderBook() : book;
        book.setScopes(scopes);
        if (book.getId() == null) {
            book.setId(word);
            bookMapper.insert(book);
        } else {
            bookMapper.update(book);
        }
    }

    public void rectBook() {
        List<WordLoaderBook> ws = bookMapper.notInFreq();
        ws.forEach(w -> {
            try {
                Document node = WordDictLoader.fetchDocument("https://dict.youdao.com/result?lang=en&word=" + w.getId());
                boolean empty = XPaths.of(node, "//div[@class='maybe']").asArray().isEmpty();
                if (!empty) {
                    System.out.println(w.getId());
                }
            } catch (Exception ex) {
                //do nothing
            }
        });
    }

    public String suggest(String w) {
        return client.target("http://dict.youdao.com/suggest")
                .queryParam("num", 1)
                .queryParam("doctype", "json")
                .queryParam("q", w)
                .request()
                .get()
                .readEntity(String.class);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Node {
        private String word;
        private int version;
        private boolean merged;
        private String parent;
        private List<Node> children;
        private int index;
    }
}