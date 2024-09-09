package com.github.sun.word.loader;

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
@Table(name = "word_loader_diff")
@JsonIgnoreProperties(ignoreUnknown = true)
public class WordLoaderDiff {
    @Id
    private String id;
    private String mean;
    @Converter(Handler.class)
    private List<String> words;

    public static class Handler extends JsonHandler.ListHandler<String> {
    }
}