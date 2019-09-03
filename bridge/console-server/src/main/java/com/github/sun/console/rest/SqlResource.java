package com.github.sun.console.rest;

import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.foundation.rest.AbstractResource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

@Path("/api/consoles/sql")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SqlResource extends AbstractResource {
  private static final int QUERY_MAX = 100;
  private static final int TIME_OUT = 20;

  @Autowired
  private Environment env;

  @POST
  @Path("/query")
  public ListResponse<Map<String, Object>> query(@NotNull(message = "缺少实体") QueryReq req) {
    List<Map<String, Object>> result = new ArrayList<>();
    try (Connection connection = create(env, req.database);
      Statement statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
      statement.setQueryTimeout(TIME_OUT);
      statement.setFetchSize(Integer.MIN_VALUE);
      try (ResultSet rs = statement.executeQuery(req.sql)) {
        int counter = 0;
        while (rs.next() && counter < QUERY_MAX) {
          int columnCount = rs.getMetaData().getColumnCount();
          Map<String, Object> map = new LinkedHashMap<>();
          for (int i = 1; i <= columnCount; i++) {
            int columnType = rs.getMetaData().getColumnType(i);
            String columnName = rs.getMetaData().getColumnName(i);
            Object value = rs.getObject(columnName);
            if (Types.TIMESTAMP == columnType) {
              DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
              value = format.format((Date) value);
            } else if (value instanceof String) {
              try {
                value = JSON.asJsonNode((String) value);
              } catch (Exception ex) {
                // do nothing
              }
            }
            map.put(columnName, value);
          }
          result.add(map);
          counter++;
        }
      }
    } catch (Throwable ex) {
      Map<String, Object> map = new HashMap<>();
      map.put("stackTrace", stackTraceOf(ex));
      result.add(map);
    }
    return responseOf(result);
  }

  @POST
  @Path("/write")
  public SingleResponse<String> write(@NotNull(message = "缺少实体") QueryReq req) {
    try (Connection connection = create(env, req.database);
      Statement statement = connection.createStatement()) {
      statement.setQueryTimeout(TIME_OUT);
      int count = statement.executeUpdate(req.sql);
      return responseOf(count + "条数据更新成功");
    } catch (Throwable ex) {
      return responseOf(stackTraceOf(ex));
    }
  }

  @GET
  @Path("/tables")
  public SetResponse<String> getTables(@NotNull(message = "缺少数据库") @QueryParam("database") String database) {
    Set<String> tables = new LinkedHashSet<>();
    try (Connection connection = create(env, database)) {
      String[] types = {"TABLE"};
      ResultSet rs = connection.getMetaData().getTables(null, database, "%", types);
      while (rs.next()) {
        if (database.equalsIgnoreCase(rs.getString(1))) {
          tables.add(rs.getString(3));
        }
      }
    } catch (Throwable ex) {
      tables.add(stackTraceOf(ex));
    }
    return responseOf(tables);
  }

  private String stackTraceOf(Throwable ex) {
    try (StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw)) {
      ex.printStackTrace(pw);
      return sw.toString();
    } catch (IOException ex2) {
      return ex.getMessage();
    }
  }

  private Connection create(Environment env, String database) {
    try {
      Class.forName("com.mysql.cj.jdbc.Driver");
      String url = env.getProperty("spring.datasource.admin.url");
      if (url != null) {
        url = String.format(url, database);
        String username = env.getProperty("spring.datasource.admin.username");
        String password = env.getProperty("spring.datasource.admin.password");
        return DriverManager.getConnection(url, username, password);
      }
      throw new IllegalArgumentException("missing spring.datasource.admin.url");
    } catch (ClassNotFoundException | SQLException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class QueryReq {
    @NotNull(message = "缺少数据库")
    private String database;
    @NotNull(message = "缺少SQL")
    private String sql;
  }
}
