package com.github.sun.image.layout;

import com.github.sun.layout.Category;
import com.github.sun.layout.CategoryProvider;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class ImageCategory implements CategoryProvider {
  @Override
  public Category provide() {
    return Category.builder()
      .type("image")
      .items(Arrays.asList(
        Category.Item.builder().label("大胸").name("daxiong").build(),
        Category.Item.builder().label("性感").name("xinggan").build(),
        Category.Item.builder().label("萝莉").name("luoli").build(),
        Category.Item.builder().label("熟女").name("shunv").build()
      ))
      .build();
  }
}
