package com.github.sun.qm;

import com.github.sun.foundation.boot.utility.Dates;
import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.SqlBuilder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.*;
import java.util.stream.Collectors;

@Path("/v1/qm/collection")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CollectionResource extends AbstractResource {
    private final CollectionMapper mapper;
    private final CollectionService service;
    private final GirlMapper girlMapper;
    private final SqlBuilder.Factory factory;

    @Inject
    public CollectionResource(CollectionMapper mapper,
                              GirlMapper girlMapper,
                              CollectionService service,
                              @Named("mysql") SqlBuilder.Factory factory) {
        this.mapper = mapper;
        this.girlMapper = girlMapper;
        this.service = service;
        this.factory = factory;
    }

    /**
     * 获取我的收藏
     */
    @GET
    public PageResponse<CollectionRes> list(@QueryParam("start") int start,
                                            @QueryParam("count") int count,
                                            @Context User user) {
        SqlBuilder sb = factory.create();
        Expression condition = sb.field("userId").eq(user.getId());
        int total = mapper.countByTemplate(sb.from(Collection.class).where(condition).count().template());
        if (start < total) {
            sb.clear();
            SqlBuilder.Template template = sb.from(Collection.class)
                    .where(condition)
                    .desc("createTime")
                    .limit(start, count)
                    .template();
            List<Collection> list = mapper.findByTemplate(template);
            Set<String> girlIds = list.stream().map(Collection::getGirlId).collect(Collectors.toSet());
            Map<String, Girl> girls = girlMapper.findByIds(girlIds).stream().collect(Collectors.toMap(Girl::getId, v -> v));
            return responseOf(total, list.stream().map(v -> {
                Girl g = girls.get(v.getGirlId());
                if (g != null) {
                    return CollectionRes.builder()
                            .id(v.getId())
                            .girlId(g.getId())
                            .name(g.getName())
                            .city(g.getCity())
                            .type(g.getType())
                            .mainImage(g.getMainImage())
                            .onService(g.isOnService())
                            .time(Dates.simpleTime(v.getUpdateTime()))
                            .build();
                }
                return null;
            }).filter(Objects::nonNull).collect(Collectors.toList()));
        }
        return responseOf(total, Collections.emptyList());
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class CollectionRes {
        private String id;
        private String girlId;
        private String name;
        private String city;
        private Girl.Type type;
        private String mainImage;
        private String time;
        private boolean onService;
    }

    /**
     * 添加
     */
    @POST
    public Response add(@Valid @NotNull(message = "require body") CollectionReq req,
                        @Context User user) {
        service.add(user.getId(), req.getGirlId());
        return responseOf();
    }

    @Data
    private static class CollectionReq {
        @NotEmpty(message = "缺少教师")
        private String girlId;
    }

    /**
     * 添加
     */
    @DELETE
    @Path("/byGirlId")
    public Response delete(@Valid @NotNull(message = "require body") CollectionReq req,
                           @Context User user) {
        mapper.deleteByUserIdAndGirlId(user.getId(), req.getGirlId());
        return responseOf();
    }

    /**
     * 删除
     */
    @DELETE
    @Path("/${id}")
    public Response delete(@PathParam("id") String id,
                           @Context User user) {
        mapper.deleteById(id);
        return responseOf();
    }
}
