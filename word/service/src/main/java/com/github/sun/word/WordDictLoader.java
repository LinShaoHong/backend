package com.github.sun.word;

import com.github.sun.foundation.ai.Assistant;
import com.github.sun.foundation.boot.Scanner;
import com.github.sun.foundation.boot.utility.Dates;
import com.github.sun.foundation.boot.utility.Reflections;
import com.github.sun.word.loader.WordBasicLoader;
import com.github.sun.word.loader.WordDerivativesLoader;
import com.github.sun.word.loader.WordStructLoader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RefreshScope
public class WordDictLoader {
  private final static ExecutorService executor = Executors.newFixedThreadPool(10);
  @Value("${qwen.key}")
  private String apiKey;
  @Value("${qwen.model}")
  private String model;
  @Resource(name = "qwen")
  private Assistant assistant;
  @Resource
  private WordDictMapper mapper;
  @Resource
  private WordCheckerMapper checkerMapper;
  @Resource
  private WordStructLoader structLoader;
  @Resource
  private WordDerivativesLoader derivativesLoader;

  public String chat(String q) {
    return assistant.chat(apiKey, model, q);
  }

  public void loadAll(String words) {
    for (String word : words.split(",")) {
      WordBasicLoader.init(word);
      Scanner.getClassesWithInterface(WordLoader.class)
        .stream()
        .filter(Scanner.ClassTag::isImplementClass)
        .filter(v -> v.runtimeClass() != WordStructLoader.class &&
          v.runtimeClass() != WordDerivativesLoader.class)
        .forEach(loader -> executor.submit(() -> loader.getInstance().load(word)));
      executor.submit(() -> {
        structLoader.load(word);
        derivativesLoader.load(word);
      });
    }
  }

  public void loadPart(String word, String part) {
    WordBasicLoader.init(word);
    mapper.loading(word, "'$." + part + "Loading'");
    Scanner.getClassesWithInterface(WordLoader.class)
      .stream().filter(v -> v.isImplementClass() &&
        v.runtimeClass().getAnnotation(Service.class).value().equals(part))
      .findFirst()
      .ifPresent(loader -> executor.submit(() -> loader.getInstance().load(word)));
  }

  @Transactional
  public void removePart(String word, String part, String path) {
    WordDict dict = WordBasicLoader.init(word);
    switch (part) {
      case "meaning":
        if (StringUtils.hasText(path)) {
          Reflections.setValue(dict.getMeaning(), path, "");
        } else {
          dict.setMeaning(new WordDict.TranslatedMeaning());
        }
        break;
      case "inflection":
        if (StringUtils.hasText(path)) {
          Reflections.setValue(dict.getInflection(), path, Collections.emptyList());
        } else {
          dict.setInflection(new WordDict.Inflection());
        }
        break;
      case "derivatives":
        if (StringUtils.hasText(path)) {
          dict.getDerivatives().removeIf(d -> d.getWord().contains(path));
        } else {
          dict.setDerivatives(Collections.emptyList());
        }
        break;
      default:
        break;
    }
    dict.setPassed(false);
    mapper.update(dict);
  }

  @Transactional
  public void pass(String word) {
    mapper.pass(word);
    WordDict dict = mapper.findById(word);
    String date = Dates.format(dict.getLoadTime());
    WordChecker checker = stat(date);
    checker.setPassed(mapper.countByPassed(date));
    checkerMapper.update(checker);
  }

  @Transactional
  public WordChecker stat(String date) {
    WordChecker checker = checkerMapper.findById(date);
    if (checker == null) {
      List<WordChecker> all = checkerMapper.all();
      checker = checkerMapper.all().get(all.size() - 1);
    }
    checker.setTotal(mapper.countByDate(checker.getId()));
    checker.setPassed(mapper.countByPassed(checker.getId()));
    return checker;
  }

  public List<WordChecker> stats() {
    return checkerMapper.all();
  }

  @Transactional
  public WordDict byDate(String date, Integer sort) {
    WordChecker checker = checkerMapper.findById(date);
    if (sort == null) {
      sort = checker == null ? 1 : checker.getSort();
    }
    WordDict dict = mapper.byDateAndSort(date, sort);
    int total = mapper.countByDate(date);
    if (dict != null) {
      if (checker == null) {
        checker = new WordChecker();
        checker.setId(date);
        checker.setSort(sort);
        checker.setViewed(1);
        checker.setTotal(total);
        checkerMapper.insert(checker);
      } else {
        checker.setSort(sort);
        checker.setTotal(total);
        if (!dict.isViewed()) {
          checker.setViewed(checker.getViewed() + 1);
        }
        checkerMapper.update(checker);
      }
      mapper.viewed(dict.getId());
    }
    return dict;
  }

  public List<WordDict> dicts(String date) {
    return mapper.byDate(date);
  }
}