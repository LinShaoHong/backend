package com.github.sun.mall.admin;

import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.mall.core.IssueMapper;
import com.github.sun.mall.core.entity.Issue;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;

@Path("/v1/mall/admin/issue")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-admin: 问题管理: issue")
public class AdminIssueResource extends BasicCURDResource<Issue, IssueMapper> {
  private final IssueMapper mapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public AdminIssueResource(IssueMapper mapper, @Named("mysql") SqlBuilder.Factory factory) {
    this.mapper = mapper;
    this.factory = factory;
  }

  @GET
  @ApiOperation("分页获取问题列表")
  public PageResponse<Issue> list(@QueryParam("question") String question,
                                  @QueryParam("username") String username,
                                  @QueryParam("start") int start,
                                  @QueryParam("count") int count,
                                  @QueryParam("sort") @DefaultValue("createTime") String sort,
                                  @QueryParam("asc") boolean asc) {
    SqlBuilder sb = factory.create();
    Expression condition = Expression.nonNull(question).then(sb.field("userId").contains(question));
    int total = mapper.countByTemplate(sb.from(Issue.class).where(condition).count().template());
    if (start < total) {
      SqlBuilder.Template template = sb.from(Issue.class)
        .where(condition)
        .orderBy(sort, asc)
        .limit(start, count)
        .template();
      final List<Issue> list = mapper.findByTemplate(template);
      return responseOf(total, list);
    }
    return responseOf(total, Collections.emptyList());
  }

  @Override
  protected String name() {
    return "问题";
  }
}
