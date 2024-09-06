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
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "word_loader_book")
@JsonIgnoreProperties(ignoreUnknown = true)
public class WordLoaderBook {
    @Id
    private String id;
    @Converter(ScopesHandler.class)
    private List<Scope> scopes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Scope {
        private String tag;
        private Set<String> names;
    }

    public static class ScopesHandler extends JsonHandler.ListHandler<Scope> {
    }
}