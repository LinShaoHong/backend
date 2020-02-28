package com.github.sun.mall.core;

import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.mall.core.entity.Keyword;
import com.github.sun.mall.core.entity.SearchHistory;
import com.github.sun.mall.core.resolver.LoginUser;
import com.github.sun.mall.core.resolver.MayLoginUser;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Builder;
import lombok.Data;

import javax.inject.Inject;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Path("/v1/mall/search")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall: Search history Resource", tags = "搜索历史服务")
public class SearchHistoryResource extends AbstractResource {
  private final SearchHistoryMapper mapper;
  private final KeywordMapper keywordMapper;

  @Inject
  public SearchHistoryResource(SearchHistoryMapper mapper, KeywordMapper keywordMapper) {
    this.mapper = mapper;
    this.keywordMapper = keywordMapper;
  }

  @GET
  @ApiOperation("获取用户搜索历史")
  public SingleResponse<SearchResp> index(@Context MayLoginUser user) {
    List<Keyword> hots = keywordMapper.findHot();
    List<Keyword> defaults = keywordMapper.findDefault();

    List<SearchHistory> historyList = Collections.emptyList();
    if (user.login()) {
      historyList = mapper.findByUserId(user.getId());
    }
    return responseOf(SearchResp.builder()
      .defaults(defaults)
      .hots(hots)
      .histories(historyList)
      .build());
  }

  @Data
  @Builder
  private static class SearchResp {
    private List<Keyword> hots;
    private List<Keyword> defaults;
    private List<SearchHistory> histories;
  }

  @GET
  @Path("/hint")
  @ApiOperation(" 关键字提醒")
  public ListResponse<String> helper(@QueryParam("start") int start,
                                     @QueryParam("count") int count,
                                     @NotEmpty(message = "缺少关键字") String keyword) {
    List<Keyword> keywords = keywordMapper.findHint(keyword, start, count);
    return responseOf(keywords.stream().map(Keyword::getKeyword).collect(Collectors.toList()));
  }

  @DELETE
  @ApiOperation("清除用户搜索历史")
  public Response clear(@Context LoginUser user) {
    mapper.deleteByUserId(user.getId());
    return responseOf();
  }
}
