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
@Table(name = "word_dict_lemma")
@JsonIgnoreProperties(ignoreUnknown = true)
public class WordDictLemma {
    @Id
    private String id;
    @Converter(Handler.class)
    private List<String> inflections;
    private int sort;

    public static class Handler extends JsonHandler.ListHandler<String> {
    }
}