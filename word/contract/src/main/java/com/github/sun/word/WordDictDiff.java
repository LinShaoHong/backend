package com.github.sun.word;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.sun.foundation.modelling.Converter;
import com.github.sun.foundation.modelling.NamingStrategy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Id;
import javax.persistence.Table;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "word_dict_diff")
@JsonIgnoreProperties(ignoreUnknown = true)
public class WordDictDiff {
  @Id
  private String id;
  private String definition;
  private String scenario;
  @Converter(WordDict.ExamplesHandler.class)
  private List<WordDict.ExampleSentence> examples;
}