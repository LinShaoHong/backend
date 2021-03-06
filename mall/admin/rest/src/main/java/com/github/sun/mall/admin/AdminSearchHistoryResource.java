package com.github.sun.mall.admin;

import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.mall.admin.auth.Authentication;
import com.github.sun.mall.admin.entity.Admin;
import com.github.sun.mall.core.SearchHistoryMapper;
import com.github.sun.mall.core.entity.Keyword;
import com.github.sun.mall.core.entity.SearchHistory;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;

@Path("/v1/mall/admin/searchHistory")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-admin: 搜索历史管理: search")
public class AdminSearchHistoryResource extends AbstractResource {
  private final SearchHistoryMapper mapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public AdminSearchHistoryResource(SearchHistoryMapper mapper, @Named("mysql") SqlBuilder.Factory factory) {
    this.mapper = mapper;
    this.factory = factory;
  }

  @GET
  @ApiOperation("分页获取搜索历史列表")
  @Authentication(value = "admin:searchHistory:query", tags = {"用户管理", "搜索历史", "查询"})
  public PageResponse<SearchHistory> paged(@QueryParam("userId") String userId,
                                           @QueryParam("keyword") String keyword,
                                           @QueryParam("start") int start,
                                           @QueryParam("count") int count,
                                           @QueryParam("sort") @DefaultValue("createTime") String sort,
                                           @QueryParam("asc") boolean asc,
                                           @Context Admin admin) {
    SqlBuilder sb = factory.create();
    Expression condition = Expression.nonNull(userId).then(sb.field("userId").eq(userId))
      .and(keyword == null ? null : sb.field("keyword").contains(keyword));
    int total = mapper.countByTemplate(sb.from(SearchHistory.class).where(condition).count().template());
    if (start < total) {
      sb.clear();
      SqlBuilder.Template template = sb.from(Keyword.class)
        .where(condition)
        .orderBy(sort, asc)
        .limit(start, count)
        .template();
      final List<SearchHistory> list = mapper.findByTemplate(template);
      return responseOf(total, list);
    }
    return responseOf(total, Collections.emptyList());
  }
}
