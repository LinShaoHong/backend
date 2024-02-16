package com.github.sun.card;

import com.github.sun.foundation.rest.AbstractResource;

import javax.inject.Inject;
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
  public Response shuffle(@QueryParam("mainUserId") String mainUserId, @QueryParam("userId") String userId) {
    service.shuffle(mainUserId, userId);
    return responseOf();
  }

  @GET
  @Path("/open")
  public Response open(@QueryParam("mainUserId") String mainUserId,
                       @QueryParam("userId") String userId,
                       @QueryParam("index") int index,
                       @QueryParam("music") boolean music) {
    service.open(mainUserId, userId, index, music);
    return responseOf();
  }

  @GET
  @Path("/close")
  public Response close(@QueryParam("mainUserId") String mainUserId) {
    service.close(mainUserId);
    return responseOf();
  }

  @GET
  @Path("/next")
  public Response next(@QueryParam("mainUserId") String mainUserId,
                       @QueryParam("playerId") String playerId) {
    service.next(mainUserId, playerId);
    return responseOf();
  }

  @GET
  @Path("/leave")
  public Response leave(@QueryParam("mainUserId") String mainUserId,
                        @QueryParam("userId") String userId) {
    service.leave(mainUserId, userId);
    return responseOf();
  }

  @GET
  @Path("/assign")
  public Response assign(@QueryParam("mainUserId") String mainUserId,
                         @QueryParam("userId") String userId) {
    service.assign(mainUserId, userId);
    return responseOf();
  }

  @GET
  @Path("/players")
  public ListResponse<CardRoomService.Player> players(@QueryParam("mainUserId") String mainUserId) {
    return responseOf(service.players(mainUserId));
  }

  @GET
  @Path("/player")
  public SingleResponse<CardRoomService.Player> player(@QueryParam("mainUserId") String mainUserId) {
    return responseOf(service.player(mainUserId));
  }

  @GET
  @Path("/joined")
  public ListResponse<CardRoomService.JoinedRoom> joined(@QueryParam("userId") String userId) {
    return responseOf(service.joined(userId));
  }

  @GET
  @Path("sub")
  @Produces(MediaType.SERVER_SENT_EVENTS)
  public void sub(@QueryParam("mainUserId") String mainUserId,
                  @QueryParam("userId") String userId,
                  @Context SseEventSink sink,
                  @Context Sse sse) {
    service.sub(mainUserId, userId, sink, sse);
  }
}