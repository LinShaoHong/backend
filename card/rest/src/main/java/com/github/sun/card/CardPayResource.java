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
import javax.ws.rs.core.MediaType;

@Path("/pay")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CardPayResource extends AbstractResource {
  private final CardPayService service;

  @Inject
  public CardPayResource(CardPayService service) {
    this.service = service;
  }

  @POST
  @Path("/wx")
  public SingleResponse<CardPayService.PayResp> wxPay(@Valid PayReq q) {
    return responseOf(service.wxPay(q.getUserId(), q.getAmount()));
  }

  @POST
  @Path("/wx/notify")
  public String wxPayNotify() {
    return "<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>";
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