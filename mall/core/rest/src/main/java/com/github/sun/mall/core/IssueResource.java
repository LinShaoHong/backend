package com.github.sun.mall.core;

import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.mall.core.entity.Issue;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;

@Path("/v1/mall/issue")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-core: 问题服务: issue")
public class IssueResource extends AbstractResource {
  private final IssueMapper mapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public IssueResource(IssueMapper mapper, @Named("mysql") SqlBuilder.Factory factory) {
    this.mapper = mapper;
    this.factory = factory;
  }

  @GET
  @ApiOperation("帮助中心")
  public PageResponse<Issue> list(@QueryParam("start") int start,
                                  @QueryParam("count") int count,
                                  @QueryParam("q") String q,
                                  @QueryParam("sort") @DefaultValue("createTime") String sort,
                                  @QueryParam("asc") boolean asc) {
    SqlBuilder sb = factory.create();
    int total = mapper.countByTemplate(sb.from(Issue.class)
      .where(q == null ? null : sb.field("question").contains(q))
      .count()
      .template());
    if (start < total) {
      sb.clear();
      SqlBuilder.Template template = sb.from(Issue.class)
        .where(q == null ? null : sb.field("question").contains(q))
        .orderBy(sort, asc)
        .limit(start, count)
        .template();
      List<Issue> list = mapper.findByTemplate(template);
      return responseOf(total, list);
    }
    return responseOf(total, Collections.emptyList());
  }
}
