package com.github.sun.word;

import com.github.sun.foundation.ai.Assistant;
import com.github.sun.foundation.boot.Scanner;
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
import java.util.stream.Collectors;

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
  private WordCheckMapper checkMapper;
  @Resource
  private WordStructLoader structLoader;
  @Resource
  private WordDerivativesLoader derivativesLoader;

  public String chat(String q) {
    return assistant.chat(apiKey, model, q);
  }

  public void loadAll(String words, int userId) {
    for (String word : words.split(",")) {
      WordBasicLoader.init(word, userId);
      Scanner.getClassesWithInterface(WordLoader.class)
        .stream()
        .filter(Scanner.ClassTag::isImplementClass)
        .filter(v -> v.runtimeClass() != WordStructLoader.class &&
          v.runtimeClass() != WordDerivativesLoader.class)
        .forEach(loader -> executor.submit(() -> loader.getInstance().load(word, userId)));
      executor.submit(() -> {
        structLoader.load(word, userId);
        derivativesLoader.load(word, userId);
      });
    }
  }

  public void loadPart(String word, String part, int userId) {
    WordBasicLoader.init(word, userId);
    mapper.loading(word, "'$." + part + "Loading'");
    Scanner.getClassesWithInterface(WordLoader.class)
      .stream().filter(v -> v.isImplementClass() &&
        v.runtimeClass().getAnnotation(Service.class).value().equals(part))
      .findFirst()
      .ifPresent(loader -> executor.submit(() -> loader.getInstance().load(word, userId)));
  }

  @Transactional
  public void removePart(String word, String part, String path, int userId) {
    WordDict dict = WordBasicLoader.init(word, userId);
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
  }

  @Transactional
  public WordCheck stat(String date, int userId) {
    WordCheck check = checkMapper.findById(date + ":" + userId);
    check.setTotal(mapper.countByDate(date));
    check.setPassed(mapper.countByPassed(date));
    check.setViewed(mapper.countByViewed(date));
    return check;
  }

  @Transactional
  public List<WordCheck> stats(int userId) {
    List<String> dates = checkMapper.dates();
    return dates.stream().map(date -> {
      String id = date + ":" + userId;
      WordCheck check = checkMapper.findById(id);
      if (check == null) {
        check = new WordCheck();
        check.setId(date + ":" + userId);
        check.setUserId(userId);
        check.setDate(date);
        check.setSort(1);
        checkMapper.replace(check);
      }
      check.setTotal(mapper.countByDate(date));
      check.setPassed(mapper.countByPassed(date));
      check.setViewed(mapper.countByViewed(date));
      return check;
    }).collect(Collectors.toList());
  }

  @Transactional
  public WordDict dict(String date, Integer sort, int userId) {
    WordCheck check = null;
    List<WordCheck> all = stats(userId);
    if (!StringUtils.hasText(date)) {
      check = all.stream().filter(WordCheck::isCurr).findFirst().orElse(null);
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
}