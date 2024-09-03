package com.github.sun.card;

import com.github.sun.foundation.rest.AbstractResource;
import lombok.Data;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/def")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CardUserDefResource extends AbstractResource {
    private final CardUserDefService service;

    @Inject
    public CardUserDefResource(CardUserDefService service) {
        this.service = service;
    }

    @GET
    @Path("/byUserId")
    public SingleResponse<DefResp> byId(@QueryParam("userId") String userId) {
        CardUserDef def = service.byUserId(userId);
        DefResp resp = new DefResp();
        resp.setUserId(def.getUserId());
        resp.setDefs(def.getDefs());
        return responseOf(resp);
    }

    @Data
    public static class DefResp {
        private String userId;
        private List<CardUserDef.Def> defs;
    }

    @POST
    @Path("/add")
    public Response add(@Valid AddDefReq q) {
        service.add(q.getUserId(), q.getTitle(), q.getContent(), q.getSrc(), q.getCardType());
        return responseOf();
    }

    @POST
    @Path("/edit")
    public Response edit(@Valid EditDefReq q) {
        service.edit(q.getUserId(), q.getItemId(), q.getTitle(), q.getContent(), q.getSrc(), q.getCardType());
        return responseOf();
    }

    @POST
    @Path("/delete")
    public Response delete(@Valid DeleteReq q) {
        service.delete(q.getUserId(), q.getItemId(), q.getCardType());
        return responseOf();
    }

    @POST
    @Path("/enable")
    public Response enable(@Valid EnableReq q) {
        service.enable(q.getUserId(), q.getItemId(), q.isEnable(), q.getCardType());
        return responseOf();
    }

    @Data
    public static class AddDefReq {
        private String userId;
        private String title;
        private String content;
        private String src;
        private String cardType;
    }

    @Data
    public static class EditDefReq {
        private String userId;
        private String itemId;
        private String title;
        private String content;
        private String src;
        private String cardType;
    }

    @Data
    public static class DeleteReq {
        private String userId;
        private String itemId;
        private String cardType;
    }

    @Data
    public static class EnableReq {
        private String userId;
        private String itemId;
        private boolean enable;
        private String cardType;
    }
}