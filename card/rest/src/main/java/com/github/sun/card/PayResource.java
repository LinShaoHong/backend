package com.github.sun.card;

import com.github.sun.foundation.rest.AbstractResource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;

@Path("/pay")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PayResource extends AbstractResource {
  private final PaymentService service;
  private final ContainerRequestContext ctx;

  @Inject
  public PayResource(PaymentService service, ContainerRequestContext ctx) {
    this.service = service;
    this.ctx = ctx;
  }

  @POST
  @Path("/wx")
  public SingleResponse<PaymentService.PayResp> wxPay(@Valid PayReq q) {
    return responseOf(service.wxPay(q.getUserId(), q.getAmount(), ctx));
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PayReq {
    private String userId;
    private String amount;
  }
}