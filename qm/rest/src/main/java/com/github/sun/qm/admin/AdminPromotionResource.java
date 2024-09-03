package com.github.sun.qm.admin;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.sql.IdGenerator;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.qm.*;
import lombok.Data;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Path("/v1/qm/admin/promotion")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AdminPromotionResource extends AdminBasicResource {
    private final PromotionMapper mapper;
    private final PayLogMapper payLogMapper;
    private final SqlBuilder.Factory factory;

    @Inject
    public AdminPromotionResource(PromotionMapper mapper,
                                  UserMapper userMapper,
                                  GirlMapper girlMapper,
                                  PayLogMapper payLogMapper,
                                  @Named("mysql") SqlBuilder.Factory factory) {
        super(userMapper, girlMapper);
        this.mapper = mapper;
        this.payLogMapper = payLogMapper;
        this.factory = factory;
    }

    /**
     * 推广列表
     */
    @GET
    public PageResponse<ObjectNode> paged(@QueryParam("start") int start,
                                          @QueryParam("count") int count,
                                          @QueryParam("status") String status,
                                          @QueryParam("userName") String userName,
                                          @QueryParam("rank") @DefaultValue("createTime") String rank,
                                          @Context Admin admin) {
        String userId = null;
        if (userName != null && !userName.isEmpty()) {
            userId = userMapper.findIdByUsername(userName.trim());
        }
        SqlBuilder sb = factory.create();
        Expression condition = Expression.EMPTY
                .and(userId == null || userId.isEmpty() ? null : sb.field("userId").eq(userId))
                .and(status == null || status.isEmpty() ? null : sb.field("status").eq(status));
        int total = mapper.countByTemplate(sb.from(Promotion.class).where(condition).count().template());
        if (start < total) {
            sb.clear();
            SqlBuilder.Template template = sb.from(Promotion.class)
                    .where(condition)
                    .desc(rank)
                    .limit(start, count)
                    .template();
            List<Promotion> list = mapper.findByTemplate(template);
            return responseOf(total, join(list, "userId"));
        }
        return responseOf(total, Collections.emptyList());
    }

    /**
     * 拒绝
     */
    @PUT
    @Path("/${id}/reject")
    public Response reject(@PathParam("id") String id,
                           @Context Admin admin) {
        Promotion p = mapper.findById(id);
        if (p != null && p.getStatus() == Promotion.Status.APPROVING) {
            p.setStatus(Promotion.Status.REJECT);
            mapper.update(p);
        }
        return responseOf();
    }

    /**
     * 通过
     */
    @PUT
    @Path("/${id}/pass")
    public Response pass(@PathParam("id") String id,
                         PassReq req,
                         @Context Admin admin) {
        Promotion p = mapper.findById(id);
        if (p != null && p.getStatus() == Promotion.Status.APPROVING) {
            p.setStatus(Promotion.Status.PASS);
            mapper.update(p);
            User user = userMapper.findById(p.getUserId());
            BigDecimal amount = user.getAmount() == null ? new BigDecimal(0) : user.getAmount();
            user.setAmount(amount.add(req.getAward()));
            userMapper.update(user);
            PayLog payLog = PayLog.builder()
                    .id(IdGenerator.next())
                    .userId(user.getId())
                    .type(PayLog.Type.PROMOTION)
                    .amount(req.getAward())
                    .build();
            payLogMapper.insert(payLog);
        }
        return responseOf();
    }

    @Data
    private static class PassReq {
        private BigDecimal award;
    }
}
