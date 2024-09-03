package com.github.sun.card;

import com.github.sun.foundation.rest.AbstractResource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Slf4j
@Path("/sms")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CardSmsResource extends AbstractResource {
    private final CardSmsService service;

    @Inject
    public CardSmsResource(CardSmsService service) {
        this.service = service;
    }

    @GET
    @Path("/getSpecsByType")
    public ListResponse<String> byId(@QueryParam("type") String type) {
        return responseOf(service.getSpecsByType(type));
    }

    @POST
    @Path("/send")
    public Response getOpenIdByCode(@Valid SendReq req) {
        service.send(req.getUserId(), req.getFromPhone(), req.getToPhone(), req.getMessage());
        return responseOf();
    }

    @GET
    @Path("/records")
    public ListResponse<CardSmsService.Record> records(@QueryParam("userId") String userId) {
        return responseOf(service.records(userId));
    }

    @GET
    @Path("/chats")
    public ListResponse<CardSmsService.Chat> chats(@QueryParam("userId") String userId,
                                                   @QueryParam("phone") String phone) {
        return responseOf(service.chats(userId, phone));
    }

    @GET
    @Path("/recPhones")
    public ListResponse<String> recPhones(@QueryParam("userId") String userId) {
        return responseOf(service.recPhones(userId));
    }

    @Data
    public static class SendReq {
        private String userId;
        private String fromPhone;
        private String toPhone;
        private String message;
    }
}