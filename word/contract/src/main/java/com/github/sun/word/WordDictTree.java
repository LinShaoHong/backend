package com.github.sun.word;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.sun.foundation.modelling.Converter;
import com.github.sun.foundation.modelling.JsonHandler;
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
@Table(name = "word_dict_tree")
@JsonIgnoreProperties(ignoreUnknown = true)
public class WordDictTree {
  @Id
  private String id;
  private String root;
  private String rootDesc;
  @Converter(DerivativesHandler.class)
  private List<Derivative> derivatives;
  private int version;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Derivative {
    private String word;
    private int index;
    private int version;
    private boolean merged;
  }

  public static class DerivativesHandler extends JsonHandler.ListHandler<Derivative> {
  }
}