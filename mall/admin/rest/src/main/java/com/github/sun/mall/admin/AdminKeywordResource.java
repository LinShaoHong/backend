package com.github.sun.mall.admin;

import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.mall.core.KeywordMapper;
import com.github.sun.mall.core.entity.Keyword;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;

@Path("/v1/mall/admin/keyword")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-admin: 关键字管理: keyword")
public class AdminKeywordResource extends BasicCURDResource<Keyword, KeywordMapper> {
  private final KeywordMapper mapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public AdminKeywordResource(KeywordMapper mapper, @Named("mysql") SqlBuilder.Factory factory) {
    this.mapper = mapper;
    this.factory = factory;
  }

  @GET
  @ApiOperation("分页获取关键字列表")
  public PageResponse<Keyword> list(@QueryParam("keyword") String keyword,
                                    @QueryParam("url") String url,
                                    @QueryParam("start") int start,
                                    @QueryParam("count") int count,
                                    @QueryParam("sort") @DefaultValue("createTime") String sort,
                                    @QueryParam("asc") boolean asc) {
    SqlBuilder sb = factory.create();
    Expression condition = Expression.nonNull(keyword).then(sb.field("keyword").contains(keyword))
      .and(url == null ? null : sb.field("username").contains(url));
    int total = mapper.countByTemplate(sb.from(Keyword.class).where(condition).count().template());
    if (start < total) {
      SqlBuilder.Template template = sb.from(Keyword.class)
        .where(condition)
        .orderBy(sort, asc)
        .limit(start, count)
        .template();
      final List<Keyword> list = mapper.findByTemplate(template);
      return responseOf(total, list);
    }
    return responseOf(total, Collections.emptyList());
  }

  @Override
  protected String name() {
    return "关键字";
  }
}
