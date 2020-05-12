package com.github.sun.qm;

import com.github.sun.foundation.boot.utility.Dates;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.IdGenerator;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

@Path("/v1/qm/promotion")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Promotion Resource")
public class PromotionResource extends AbstractResource {
  private final PromotionMapper mapper;

  @Autowired
  private Environment env;

  @Inject
  public PromotionResource(PromotionMapper mapper) {
    this.mapper = mapper;
  }

  @GET
  @ApiOperation("查询推广记录")
  public ListResponse<PromotionRes> get(@Context User user) {
    List<Promotion> list = mapper.findByUserId(user.getId());
    return responseOf(list.stream().map(v -> PromotionRes.builder()
      .id(v.getId())
      .passed(v.getStatus() == Promotion.Status.PASS)
      .images(v.getImages())
      .applyTime(Dates.simpleTime(v.getCreateTime()))
      .build()).collect(Collectors.toList()));
  }

  @GET
  @Path("/treatment")
  @ApiOperation("获取推广文案")
  public SingleResponse<String> getTreatment(@Context User user) {
    return responseOf(env.getProperty("promotion.treatment"));
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  private static class PromotionRes {
    private String id;
    private List<String> images;
    private String applyTime;
    private boolean passed;
  }

  @POST
  @ApiOperation("提交推广")
  public Response create(@Valid @NotNull(message = "缺少实体") PromotionReq req,
                         @Context User user) {
    Promotion p = Promotion.builder()
      .id(IdGenerator.next())
      .userId(user.getId())
      .images(req.getImages())
      .status(Promotion.Status.APPROVING)
      .build();
    mapper.insert(p);
    return responseOf();
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  private static class PromotionReq {
    @NotNull(message = "缺少验证图片")
    @Size(min = 1, message = "缺少验证图片")
    private List<String> images;
  }
}
