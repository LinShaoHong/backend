package com.github.sun.word;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.sun.foundation.modelling.NamingStrategy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "word_check")
@JsonIgnoreProperties(ignoreUnknown = true)
public class WordCheck {
  @Id
  private String id;
  private String date;
  private int userId;
  private int sort;
  private boolean curr;
  @Transient
  private int passed;
  @Transient
  private int total;
}