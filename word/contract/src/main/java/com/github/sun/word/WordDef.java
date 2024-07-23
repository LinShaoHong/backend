package com.github.sun.word;

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
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "word_def")
public class WordDef {
  @Id
  private String id;
  private String ukTranscription;//英式音标
  private String usTranscription;//美式音标
  private TranslatedMeaning meaning;//中文释义
  private List<ExampleSentences> examples;//例句
  private Set<String> tags;//标签
  @Transient
  private Date createTime;
  @Transient
  private Date updateTime;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TranslatedMeaning {
    private String nouns;
    private String verbs;
    private String adjectives;
    private String adverbs;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ExampleSentences {
    private String sentence;
    private String translation;
  }
}
