package com.github.sun.word;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.sun.foundation.modelling.NamingStrategy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "word_dict_freq")
@JsonIgnoreProperties(ignoreUnknown = true)
public class WordDictFreq {
    @Id
    private String id;
    private long freq;
    private int sort;
}