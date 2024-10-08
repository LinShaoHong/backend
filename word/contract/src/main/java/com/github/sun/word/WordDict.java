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
import java.util.Date;
import java.util.List;

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
    private String ukAudioId;//英式音频
    private String ukPhonetic;//英式音标
    private String usAudioId;//美式音频
    private String usPhonetic;//美式音标
    @Converter(MeaningHandler.class)
    private TranslatedMeaning meaning;//中文释义
    @Converter(ExamplesHandler.class)
    private List<Example> examples;//例句
    @Converter(StructHandler.class)
    private Struct struct;//词根词缀结构
    private String origin;//词源历史
    @Converter(InflectionHandler.class)
    private Inflection inflection;//派生词
    @Converter(DerivativesHandler.class)
    private List<Derivative> derivatives;//派生树
    @Converter(CollocationHandler.class)
    private Collocation collocation;//短语搭配
    @Converter(SynAntHandler.class)
    private SynAnt synAnts;//近反义词
    //----------- loader ----------
    @Converter(LoadStateHandler.class)
    private LoadState loadState;//获取状态
    @Converter(FromModelHandler.class)
    private FromModel fromModel;//语言模型
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
        private String noun;
        private String verb;
        private String transitiveVerb;
        private String intransitiveVerb;
        private String auxiliaryVerb;
        private String modalVerb;
        private String adjective;
        private String adverb;
        private String preposition;
        private String pronoun;
        private String conjunction;
        private String article;
        private String interjection;
        private String numeral;
        private String determiner;
        private String abbreviation;
        private List<String> sorts;
    }

    public static class MeaningHandler extends JsonHandler<TranslatedMeaning> {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Example {
        private String speech;
        private List<ExampleSentence> sentences;
    }

    public static class ExamplesHandler extends JsonHandler.ListHandler<Example> {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExampleSentence {
        private String audioId;
        private String sentence;
        private String translation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Struct {
        private List<Part> parts;
        private String analysis;
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
    public static class Formula {
        private String en;
        private String zh;
        private List<ExampleSentence> examples;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Phrase {
        private String en;
        private String zh;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Collocation {
        private List<Formula> formulas;
        private List<Phrase> phrases;
    }

    public static class CollocationHandler extends JsonHandler<Collocation> {
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
        private boolean originLoading;
        private boolean synAntsLoading;
        private boolean derivativesLoading;
        private boolean differsLoading;
        private boolean collocationLoading;
        private boolean createTreeLoading;
        private boolean mergeTreeLoading;
    }

    public static class LoadStateHandler extends JsonHandler<LoadState> {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FromModel {
        private String meaning;
        private String examples;
        private String struct;
        private String origin;
        private String synAnts;
        private String differs;
        private String collocation;
    }

    public static class FromModelHandler extends JsonHandler<FromModel> {
    }
}