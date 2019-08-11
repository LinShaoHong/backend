package com.github.sun.layout;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {
  private static final Cache<String, List<Category>> cache = Caffeine.newBuilder()
    .expireAfterWrite(5, TimeUnit.DAYS)
    .maximumSize(100)
    .build();

  @Autowired
  private Map<String, CategoryProvider> providers;

  @Override
  public List<Category> getAll() {
    return CategoryProvider.TYPES.stream().flatMap(type -> {
      CategoryProvider provider = providers.get(type);
      if (provider != null) {
        List<Category> list = cache.get(type, t -> provider.provide());
        if (list != null) {
          return list.stream();
        }
      }
      return null;
    }).filter(Objects::nonNull).collect(Collectors.toList());
  }
}
