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
@Table(name = "word_dict_audio")
@JsonIgnoreProperties(ignoreUnknown = true)
public class WordDictAudio {
    @Id
    private String id;
    private String usPath;
    @Converter(TimesHandler.class)
    private List<TextTime> usTimes;
    private String ukPath;
    @Converter(TimesHandler.class)
    private List<TextTime> ukTimes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TextTime {
        private long start;
        private long end;
        private String text;
    }

    public static class TimesHandler extends JsonHandler.ListHandler<TextTime> {
    }
}