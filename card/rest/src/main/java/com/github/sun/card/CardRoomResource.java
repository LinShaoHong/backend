package com.github.sun.card;

import com.github.sun.foundation.rest.AbstractResource;
import lombok.Data;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;

@Path("/room")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CardRoomResource extends AbstractResource {
  private final CardRoomService service;

  @Inject
  public CardRoomResource(CardRoomService service) {
    this.service = service;
  }

  @GET
  @Path("/shuffle")
  public Response shuffle(@QueryParam("mainUserId") String mainUserId,
                          @QueryParam("userId") String userId,
                          @DefaultValue("true") @QueryParam("hks") boolean hks,
                          @DefaultValue("hks") @QueryParam("cardType") String cardType) {
    service.shuffle(mainUserId, userId, hks, cardType);
    return responseOf();
  }

  @GET
  @Path("/open")
  public Response open(@QueryParam("mainUserId") String mainUserId,
                       @QueryParam("userId") String userId,
                       @DefaultValue("true") @QueryParam("hks") boolean hks,
                       @DefaultValue("hks") @QueryParam("cardType") String cardType,
                       @QueryParam("index") int index,
                       @QueryParam("music") boolean music) {
    service.open(mainUserId, userId, hks, cardType, index, music);
    return responseOf();
  }

  @GET
  @Path("/close")
  public Response close(@QueryParam("mainUserId") String mainUserId,
                        @DefaultValue("true") @QueryParam("hks") boolean hks) {
    service.close(mainUserId, hks);
    return responseOf();
  }

  @GET
  @Path("/next")
  public Response next(@QueryParam("mainUserId") String mainUserId,
                       @DefaultValue("true") @QueryParam("hks") boolean hks,
                       @QueryParam("playerId") String playerId) {
    service.next(mainUserId, hks, playerId);
    return responseOf();
  }

  @GET
  @Path("/leave")
  public Response leave(@QueryParam("mainUserId") String mainUserId,
                        @QueryParam("userId") String userId,
                        @DefaultValue("true") @QueryParam("hks") boolean hks) {
    service.leave(mainUserId, hks, userId);
    return responseOf();
  }

  @GET
  @Path("/assign")
  public Response assign(@QueryParam("mainUserId") String mainUserId,
                         @QueryParam("userId") String userId,
                         @DefaultValue("true") @QueryParam("hks") boolean hks) {
    service.assign(mainUserId, hks, userId);
    return responseOf();
  }

  @GET
  @Path("/changeCardType")
  public Response changeCardType(@QueryParam("mainUserId") String mainUserId,
                                 @QueryParam("cardType") String cardType) {
    service.changeCardType(mainUserId, cardType);
    return responseOf();
  }

  @POST
  @Path("/reply")
  public ListResponse<CardRoomService.Chat> reply(@Valid Reply reply) {
    return responseOf(service.reply(reply.getMainUserId(), reply.getUserId(), reply.getMessage()));
  }

  @Data
  public static class Reply {
    private String mainUserId;
    private String userId;
    private String message;
  }

  @GET
  @Path("/withdrawReply")
  public Response withdrawReply(@QueryParam("mainUserId") String mainUserId,
                                @QueryParam("chatId") String chatId) {
    service.withdrawReply(mainUserId, chatId);
    return responseOf();
  }

  @GET
  @Path("/replies")
  public ListResponse<CardRoomService.Chat> replies(@QueryParam("mainUserId") String mainUserId) {
    return responseOf(service.replies(mainUserId));
  }

  @GET
  @Path("/players")
  public ListResponse<CardRoomService.Player> players(@QueryParam("mainUserId") String mainUserId,
                                                      @DefaultValue("true") @QueryParam("hks") boolean hks) {
    return responseOf(service.players(mainUserId, hks));
  }

  @GET
  @Path("/player")
  public SingleResponse<CardRoomService.Player> player(@QueryParam("mainUserId") String mainUserId,
                                                       @DefaultValue("true") @QueryParam("hks") boolean hks) {
    return responseOf(service.player(mainUserId, hks));
  }

  @GET
  @Path("/total")
  public SingleResponse<Integer> total(@QueryParam("mainUserId") String mainUserId,
                                       @QueryParam("cardType") String cardType) {
    return responseOf(service.total(mainUserId, cardType));
  }

  @GET
  @Path("/joined")
  public ListResponse<CardRoomService.JoinedRoom> joined(@QueryParam("userId") String userId,
                                                         @DefaultValue("true") @QueryParam("hks") boolean hks) {
    return responseOf(service.joined(userId, hks));
  }

  @GET
  @Path("sub")
  @Produces(MediaType.SERVER_SENT_EVENTS)
  public void sub(@QueryParam("mainUserId") String mainUserId,
                  @QueryParam("userId") String userId,
                  @DefaultValue("true") @QueryParam("hks") boolean hks,
                  @Context SseEventSink sink,
                  @Context Sse sse) {
    service.sub(mainUserId, userId, hks, sink, sse);
  }
}