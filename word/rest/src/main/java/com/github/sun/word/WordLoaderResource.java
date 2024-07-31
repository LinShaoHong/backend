package com.github.sun.word;

import com.github.sun.foundation.rest.AbstractResource;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;

@Path("/loader")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class WordLoaderResource extends AbstractResource {
  private final WordDictLoader loader;
  private final WordPdfService pdfService;

  @Inject
  public WordLoaderResource(WordDictLoader loader, WordPdfService pdfService) {
    this.loader = loader;
    this.pdfService = pdfService;
  }

  @GET
  @Path("/all")
  public Response loadAll(@QueryParam("words") String words,
                          @QueryParam("userId") int userId) {
    loader.loadAll(words, userId);
    return responseOf();
  }

  @GET
  @Path("/part")
  public Response loadPart(@QueryParam("word") String word,
                           @QueryParam("part") String part,
                           @QueryParam("userId") int userId) {
    loader.loadPart(word, part, userId);
    return responseOf();
  }

  @GET
  @Path("/remove")
  public Response loadPart(@QueryParam("word") String word,
                           @QueryParam("part") String part,
                           @QueryParam("path") String path,
                           @QueryParam("userId") int userId) {
    loader.removePart(word, part, path, userId);
    return responseOf();
  }

  @GET
  @Path("/pass")
  public Response pass(@QueryParam("word") String word) {
    loader.pass(word);
    return responseOf();
  }

  @GET
  @Path("/dict")
  public SingleResponse<WordDict> byDate(@QueryParam("date") String date,
                                         @QueryParam("sort") Integer sort,
                                         @QueryParam("userId") int userId) {
    return responseOf(loader.dict(date, sort, userId));
  }

  @GET
  @Path("/stat")
  public SingleResponse<WordCheck> stat(@QueryParam("date") String date,
                                        @QueryParam("userId") int userId) {
    return responseOf(loader.stat(date, userId));
  }

  @GET
  @Path("/stats")
  public ListResponse<WordCheck> stats(@QueryParam("userId") int userId) {
    return responseOf(loader.stats(userId));
  }

  @GET
  @Path("/dicts")
  public ListResponse<WordDict> dicts(@QueryParam("date") String date) {
    return responseOf(loader.dicts(date));
  }

  @GET
  @Path("/chat")
  public SingleResponse<String> chat(@QueryParam("q") String q) {
    return responseOf(loader.chat(q));
  }

  @POST
  @Path("/pdf")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response upload(@FormDataParam("file") InputStream in,
                         @FormDataParam("file") FormDataContentDisposition meta) throws Exception {
    pdfService.parseRoot(in);
    return responseOf();
  }
}