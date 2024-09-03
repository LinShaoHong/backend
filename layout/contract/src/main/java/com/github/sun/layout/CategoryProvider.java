package com.github.sun.layout;

import java.util.Arrays;
import java.util.List;

public interface CategoryProvider {
    String IMAGE = "image";
    List<String> TYPES = Arrays.asList(IMAGE);

    List<Category> provide();
}
