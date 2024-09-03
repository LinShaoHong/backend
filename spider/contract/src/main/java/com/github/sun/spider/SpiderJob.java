package com.github.sun.spider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.Date;
import java.util.Objects;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "spider_job")
public class SpiderJob {
    @Id
    private String id;
    private String group;
    private boolean publish;
    private long startTime;
    private String rate;
    @Converter(SettingHandler.class)
    private Setting setting;
    @Converter(JsonHandler.JsonNodeHandler.class)
    private JsonNode schema;
    @Converter(CheckpointHandler.class)
    private Spider.Checkpoint checkpoint;
    @Transient
    @JsonIgnore
    private Date createTime;
    @Transient
    @JsonIgnore
    private Date updateTime;

    public boolean needReschedule(SpiderJob updated) {
        return !Objects.equals(this.startTime, updated.getStartTime()) ||
                !Objects.equals(this.rate, updated.getRate());
    }

    public static class SettingHandler extends JsonHandler<Setting> {
    }

    public static class CheckpointHandler extends JsonHandler<Spider.Checkpoint> {
    }
}
