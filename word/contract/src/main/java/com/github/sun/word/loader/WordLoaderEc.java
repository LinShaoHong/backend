package com.github.sun.word.loader;

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
@Table(name = "word_loader_ec")
@JsonIgnoreProperties(ignoreUnknown = true)
public class WordLoaderEc {
    @Id
    private String id;
    private String phonetic;
    private String definition;
    private String translation;
    private int pos;
    private int collins;
    private int oxford;
    private String tag;
    private int bnc;
    private int frq;
    private String exchange;
    private String detail;
    private String audio;
}