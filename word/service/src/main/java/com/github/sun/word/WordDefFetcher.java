package com.github.sun.word;

import com.github.sun.foundation.ai.Assistant;
import com.ibm.icu.impl.data.ResourceReader;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import javax.inject.Named;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RefreshScope
@RequiredArgsConstructor
public class WordDefFetcher {
  @Value("${qwen.key}")
  private String apiKey;
  @Value("${qwen.model}")
  private String model;
  private final @Named("qwen") Assistant assistant;

  public void fetch(String word) {
    ClassLoader loader = ResourceReader.class.getClassLoader();
    try (InputStream in = loader.getResourceAsStream("cues/词根词缀.md")) {
      if (in != null) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String q = reader.lines().collect(Collectors.joining("\n"));
        List<String> chat = assistant.chat(apiKey, model, Arrays.asList(q, word));
        String md = chat.get(0);
        if (md.toLowerCase().startsWith("```json")) {
          md = md.substring(7, md.lastIndexOf("```"));
        }
        System.out.println(md);
      }
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }
}
