package com.github.sun.scheduler;

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
@Table(name = "scheduler_job")
public class SchedulerJob {
    @Id
    private String id;
    private long startTime;
    private String rate;
    @Converter(JsonHandler.JsonNodeHandler.class)
    private JsonNode profiles;
    private boolean publish;
    @Transient
    @JsonIgnore
    private Date createTime;
    @Transient
    @JsonIgnore
    private Date updateTime;

    public boolean needReschedule(SchedulerJob updated) {
        return !Objects.equals(this.startTime, updated.getStartTime()) ||
                !Objects.equals(this.rate, updated.getRate());
    }
}
