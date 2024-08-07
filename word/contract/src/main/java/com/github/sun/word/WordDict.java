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
import javax.persistence.Transient;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "word_dict")
@JsonIgnoreProperties(ignoreUnknown = true)
public class WordDict {
  @Id
  private String id;
  private String ukPhonetic;//英式音标
  private String usPhonetic;//美式音标
  @Converter(MeaningHandler.class)
  private TranslatedMeaning meaning;//中文释义
  @Converter(ExamplesHandler.class)
  private List<ExampleSentence> examples;//例句
  @Converter(StructHandler.class)
  private Struct struct;//组成结构
  @Converter(InflectionHandler.class)
  private Inflection inflection;//派生词
  @Converter(DerivativesHandler.class)
  private List<Derivative> derivatives;//派生树
  @Converter(DiffersHandler.class)
  private List<Differ> differs;//辨析
  @Converter(PhrasesHandler.class)
  private List<Phrase> phrases;//短语词组
  @Converter(SynAntHandler.class)
  private SynAnt synAnts;//近反义词
  private String tags;//标签
  //----------- loader ----------
  @Converter(LoadStateHandler.class)
  private LoadState loadState;//获取状态
  private boolean passed; //是否已通过
  private boolean viewed; //是否已查看
  private Date loadTime;//获取时间
  private Date passTime;//通过时间
  private Integer sort;//排序

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class TranslatedMeaning {
    private String nouns;
    private String verbs;
    private String adjectives;
    private String adverbs;
  }

  public static class MeaningHandler extends JsonHandler<TranslatedMeaning> {
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ExampleSentence {
    private String sentence;
    private String translation;
  }

  public static class ExamplesHandler extends JsonHandler.ListHandler<ExampleSentence> {
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Struct {
    private List<Part> parts;
    private String analysis;
    private String history;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Part {
    private String part;
    private boolean root;
    private boolean prefix;
    private boolean infix;
    private boolean suffix;
    private String meaning;
    private String meaningTrans;
  }

  public static class StructHandler extends JsonHandler<Struct> {
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Inflection {
    private List<String> plural;//复数
    private List<String> progressive;//进行时
    private List<String> perfect;//完成时
    private List<String> past;//过去时
    private List<String> thirdPresent;//第三人称
    private List<String> comparative;//比较级
    private List<String> superlative;//最高级
  }

  public static class InflectionHandler extends JsonHandler<Inflection> {
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Derivative {
    private String word;
    private int index;
  }

  public static class DerivativesHandler extends JsonHandler.ListHandler<Derivative> {
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Differ {
    private String word;
    private String definition;
    private String scenario;
    private List<ExampleSentence> examples;
  }

  public static class DiffersHandler extends JsonHandler.ListHandler<Differ> {
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Phrase {
    private String en;
    private String zh;

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Phrase)) return false;
      Phrase phrase = (Phrase) o;
      return getEn().equals(phrase.getEn());
    }

    @Override
    public int hashCode() {
      return Objects.hash(getEn());
    }
  }

  public static class PhrasesHandler extends JsonHandler.ListHandler<Phrase> {
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class SynAnt {
    private List<String> synonyms;
    private List<String> antonyms;
  }

  public static class SynAntHandler extends JsonHandler<SynAnt> {
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class LoadState {
    private boolean meaningLoading;
    private boolean examplesLoading;
    private boolean inflectionLoading;
    private boolean structLoading;
    private boolean synAntsLoading;
    private boolean derivativesLoading;
    private boolean differsLoading;
    private boolean phrasesLoading;
  }

  public static class LoadStateHandler extends JsonHandler<LoadState> {
  }
}