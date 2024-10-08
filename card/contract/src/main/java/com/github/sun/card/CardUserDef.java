package com.github.sun.card;

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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "card_user_def")
public class CardUserDef {
    @Id
    private String id;
    private String userId;
    @Converter(DefsHandler.class)
    private List<Def> defs;
    @Transient
    private Date createTime;
    @Transient
    private Date updateTime;

    @Data
    public static class Def {
        private String name;
        private List<Item> items;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private String id;
        private String title;
        private String content;
        private String src;
        private boolean defaulted;
        private boolean enable;
    }

    public static class DefsHandler extends JsonHandler.ListHandler<Def> {
    }

    public List<Def> getDefs() {
        if (defs == null) {
            defs = new ArrayList<>();
        }
        return defs;
    }
}