package com.github.sun.image.layout;

import com.github.sun.foundation.boot.utility.Pinyins;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.image.Image;
import com.github.sun.image.mapper.ImageCategoryMapper;
import com.github.sun.layout.Category;
import com.github.sun.layout.CategoryProvider;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component(CategoryProvider.IMAGE)
public class ImageCategory implements CategoryProvider {
  private static final List<String> ignores = Arrays.asList("网", "馆", "社", "院", "荟");

  @Resource
  private ImageCategoryMapper mapper;
  @Resource(name = "mysql")
  private SqlBuilder.Factory factory;

  @Override
  public List<Category> provide() {
    SqlBuilder sb = factory.create();
    SqlBuilder.Template template = sb.from(Image.Category.class)
      .where(sb.field("count").ge(20))
      .desc("count")
      .asc(sb.field("LENGTH").call(sb.field("name")))
      .template();
    List<Image.Category> categories = mapper.findByTemplate(template);
    Set<String> subTypes = categories.stream().map(Image.Category::getType).collect(Collectors.toSet());
    return subTypes.stream().map(t -> Category.builder()
      .type(IMAGE)
      .subType(t)
      .items(categories.stream()
        .filter(c -> c.getType().equals(t) &&
          ignores.stream().noneMatch(v -> c.getLabel().contains(v))
          && !Pinyins.isChineseSurname(c.getLabel()))
        .map(c -> Category.Item.builder()
          .label(c.getLabel())
          .name(c.getName())
          .count(c.getCount())
          .build())
        .collect(Collectors.toList()))
      .build()).collect(Collectors.toList());
  }
}
