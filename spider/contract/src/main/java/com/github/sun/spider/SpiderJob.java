package com.github.sun.spider;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.sun.foundation.mybatis.handler.JsonHandler;
import com.github.sun.foundation.sql.Handler;
import com.github.sun.foundation.sql.NamingStrategy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "spider_job")
public class SpiderJob {
  public enum Group {
    IMAGE("图片");

    public final String label;

    Group(String label) {
      this.label = label;
    }
  }

  @Id
  private String id;
  private Group group;
  private boolean publish;
  private long startTime;
  private String rate;
  @Handler(SettingHandler.class)
  private Setting setting;
  @Handler(JsonHandler.JsonNodeHandler.class)
  private JsonNode schema;
  @Transient
  private Date createTime;
  @Transient
  private Date updateTime;

  public static class SettingHandler extends JsonHandler<Setting> {
  }
}
