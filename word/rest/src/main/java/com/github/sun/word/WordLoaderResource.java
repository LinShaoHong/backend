package com.github.sun.word;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.word.spider.WordXdfSpider;
import com.github.sun.word.spider.WordXxEnSpider;
import lombok.Data;
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
  private final WordXxEnSpider xxEnAffixSpider;
  private final WordXdfSpider xdfSpider;

  @Inject
  public WordLoaderResource(WordDictLoader loader,
                            WordPdfService pdfService,
                            WordXxEnSpider xxEnAffixSpider,
                            WordXdfSpider xdfSpider) {
    this.loader = loader;
    this.pdfService = pdfService;
    this.xxEnAffixSpider = xxEnAffixSpider;
    this.xdfSpider = xdfSpider;
  }

  @GET
  @Path("/all")
  public Response loadAll(@QueryParam("words") String words,
                          @QueryParam("userId") int userId) {
    loader.loadAll(words, userId);
    return responseOf();
  }

  @POST
  @Path("/part")
  public Response loadPart(PartReq req) {
    loader.loadPart(req.getWord(), req.getPart(), req.getAttr(), req.getUserId());
    return responseOf();
  }

  @Data
  public static class PartReq {
    private String word;
    private String part;
    private String path;
    private int userId;
    private JsonNode attr;
  }

  @POST
  @Path("/edit/struct")
  public Response editStruct(EditStructReq req) {
    loader.editStruct(req.getId(), req.getStruct());
    return responseOf();
  }

  @Data
  public static class EditStructReq {
    private String id;
    private WordDict.Struct struct;
  }

  @POST
  @Path("/edit/meaning")
  public Response editMeaning(EditMeaningReq req) {
    loader.editMeaning(req.getId(), req.getMeaning());
    return responseOf();
  }

  @Data
  public static class EditMeaningReq {
    private String id;
    private WordDict.TranslatedMeaning meaning;
  }

  @GET
  @Path("/move/derivative")
  public Response moveDerivative(@QueryParam("id") String id,
                                 @QueryParam("word") String word,
                                 @QueryParam("op") String op) {
    loader.moveDerivative(id, word, op);
    return responseOf();
  }

  @GET
  @Path("/add/derivative")
  public Response addDerivative(@QueryParam("id") String id,
                                @QueryParam("word") String word,
                                @QueryParam("input") String input) {
    loader.addDerivative(id, word, input);
    return responseOf();
  }

  @GET
  @Path("/remove/part")
  public Response removePart(@QueryParam("word") String word,
                             @QueryParam("part") String part,
                             @QueryParam("path") String path,
                             @QueryParam("userId") int userId) {
    loader.removePart(word, part, path, userId);
    return responseOf();
  }

  @GET
  @Path("/remove")
  public SingleResponse<Integer> remove(@QueryParam("word") String word) {
    return responseOf(loader.remove(word));
  }

  @GET
  @Path("/pass")
  public Response pass(@QueryParam("word") String word) {
    loader.pass(word);
    return responseOf();
  }

  @GET
  @Path("/dict")
  public SingleResponse<WordDict> dict(@QueryParam("date") String date,
                                       @QueryParam("sort") Integer sort,
                                       @QueryParam("userId") int userId) {
    return responseOf(loader.dict(date, sort, userId));
  }

  @GET
  @Path("/root")
  public SingleResponse<String> root(@QueryParam("word") String word) {
    return responseOf(loader.root(word));
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
  @Path("/spider/affix/xxEn")
  public Response spiderXxEnAffix() throws Exception {
    xxEnAffixSpider.fetchAffix();
    return responseOf();
  }

  @GET
  @Path("/affix")
  public SingleResponse<WordAffix> affix(@QueryParam("word") String word) {
    return responseOf(loader.affix(word));
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

  @GET
  @Path("/tag")
  public Response tag(@QueryParam("uri") String uri,
                      @QueryParam("start") int start,
                      @QueryParam("end") int end) {
    xdfSpider.fetchWords(uri, start, end);
    return responseOf();
  }
}