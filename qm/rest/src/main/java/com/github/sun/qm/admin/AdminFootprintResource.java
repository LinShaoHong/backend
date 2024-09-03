package com.github.sun.qm.admin;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.qm.Footprint;
import com.github.sun.qm.FootprintMapper;
import com.github.sun.qm.GirlMapper;
import com.github.sun.qm.UserMapper;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;

@Path("/v1/qm/admin/footprints")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AdminFootprintResource extends AdminBasicResource {
    private final FootprintMapper mapper;
    private final SqlBuilder.Factory factory;

    @Inject
    public AdminFootprintResource(UserMapper userMapper,
                                  GirlMapper girlMapper,
                                  FootprintMapper mapper,
                                  @Named("mysql") SqlBuilder.Factory factory) {
        super(userMapper, girlMapper);
        this.mapper = mapper;
        this.factory = factory;
    }

    /**
     * 分页获取浏览记录
     */
    @GET
    public PageResponse<ObjectNode> paged(@QueryParam("start") int start,
                                          @QueryParam("count") int count,
                                          @QueryParam("userName") String userName,
                                          @QueryParam("rank") @DefaultValue("updateTime") String rank,
                                          @Context Admin admin) {
        String userId = null;
        if (userName != null && !userName.isEmpty()) {
            userId = userMapper.findIdByUsername(userName.replaceAll(" ", ""));
        }
        SqlBuilder sb = factory.create();
        Expression condition = Expression.EMPTY
                .and(userId == null || userId.isEmpty() ? null : sb.field("userId").eq(userId));
        int total = mapper.countByTemplate(sb.from(Footprint.class).where(condition).count().template());
        if (start < total) {
            sb.clear();
            SqlBuilder.Template template = sb.from(Footprint.class)
                    .where(condition)
                    .desc(rank)
                    .limit(start, count)
                    .template();
            List<Footprint> list = mapper.findByTemplate(template);
            return responseOf(total, join(list, "userId", "girlId"));
        }
        return responseOf(total, Collections.emptyList());
    }
}
