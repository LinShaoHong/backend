package com.github.sun.qm.admin;

import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.qm.StorageService;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;

@Path("/v1/qm/admin/store")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AdminStorageResource extends AbstractResource {
    private final StorageService service;

    @Inject
    public AdminStorageResource(StorageService service) {
        this.service = service;
    }

    /**
     * 上传
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public SingleResponse<String> uploadImage(@FormDataParam("file") InputStream in,
                                              @FormDataParam("file") FormDataContentDisposition meta,
                                              @Context Admin admin) {
        try {
            return responseOf(service.upload(in, meta.getFileName()));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * 删除
     */
    @POST
    @Path("/delete")
    public Response deleteImage(@Valid @NotNull DeleteReq path,
                                @Context Admin admin) {
        service.delete(path.getPath());
        return responseOf();
    }

    @Data
    private static class DeleteReq {
        @NotEmpty
        private String path;
    }
}
