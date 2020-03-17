package com.github.sun.xfg.admin;

import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.xfg.StorageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;

@Path("/v1/xfg/admin/store")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Admin Store Resource")
public class AdminStorageResource extends AbstractResource {
  private final StorageService service;

  @Inject
  public AdminStorageResource(StorageService service) {
    this.service = service;
  }

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @ApiOperation("上传图片")
  public SingleResponse<String> uploadImage(@FormDataParam("file") InputStream in,
                                            @FormDataParam("file") FormDataContentDisposition meta) {
    try {
      return responseOf(service.upload(in, meta.getFileName()));
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @POST
  @Path("/delete")
  @ApiOperation("删除图片")
  public Response deleteImage(@Valid @NotNull DeleteReq path) {
    service.delete(path.getPath());
    return responseOf();
  }

  @Data
  private static class DeleteReq {
    @NotEmpty
    private String path;
  }
}
