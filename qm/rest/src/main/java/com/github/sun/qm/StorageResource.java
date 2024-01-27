package com.github.sun.qm;

import com.github.sun.foundation.boot.exception.Message;
import com.github.sun.foundation.rest.AbstractResource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Path("/v1/qm/storage")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class StorageResource extends AbstractResource {
  private final StorageService storageService;

  @Inject
  public StorageResource(StorageService storageService) {
    this.storageService = storageService;
  }

  /**
   * 上传文件
   */
  @POST
  @Path("/upload")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public SingleResponse<String> upload(@FormDataParam("file") InputStream in,
                                       @FormDataParam("file") FormDataContentDisposition meta,
                                       @Context User user) {
    try {
      String path = storageService.upload(in, meta.getFileName());
      return responseOf(path);
    } catch (IOException ex) {
      log.error("Upload File Error: \n", ex);
      throw new Message(5001);
    }
  }

  /**
   * 删除文件
   */
  @DELETE
  public Response delete(@Valid DeleteReq path) {
    final String p = path.getPath();
    if (p != null && !p.isEmpty()) {
      storageService.delete(p);
    }
    return responseOf();
  }

  @Data
  private static class DeleteReq {
    private String path;
  }
}
