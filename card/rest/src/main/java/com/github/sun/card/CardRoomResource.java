package com.github.sun.card;

import com.github.sun.foundation.rest.AbstractResource;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;

@Path("/room")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CardRoomResource extends AbstractResource {

  @GET
  @Produces(MediaType.SERVER_SENT_EVENTS)
  public void shuffle(@QueryParam("id") String id,
                      @Context SseEventSink sink,
                      @Context Sse sse) {
    new Thread(() -> {
      for (int i = 0; i < 10; i++) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        final OutboundSseEvent event =
          sse.newEventBuilder().name("message-to-client")
            .data(String.class, "Hello world " + i + "!")
            .build();
        if (sink.isClosed()) {
          return;
        }
        sink.send(event);
        System.out.println("sending," + i);
      }
      sink.close();
    }).start();
  }
}