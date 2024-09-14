package com.github.sun.word;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.word.loader.WordLoaderAffix;
import com.github.sun.word.loader.WordLoaderCheck;
import lombok.Data;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/loader")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class WordLoaderResource extends AbstractResource {
    private final WordDictLoader loader;

    @Inject
    public WordLoaderResource(WordDictLoader loader) {
        this.loader = loader;
    }

    @GET
    @Path("/fetch")
    public Response fetch(@QueryParam("userId") int userId) {
        loader.fetch(userId);
        return responseOf();
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
    public SingleResponse<WordDictTree> moveDerivative(@QueryParam("id") String id,
                                                       @QueryParam("version") int version,
                                                       @QueryParam("word") String word,
                                                       @QueryParam("op") String op) {
        return responseOf(loader.moveDerivative(id, version, word, op));
    }

    @GET
    @Path("/add/derivative")
    public SingleResponse<WordDictTree> addDerivative(@QueryParam("id") String id,
                                                      @QueryParam("word") String word,
                                                      @QueryParam("input") String input,
                                                      @QueryParam("version") int version) {
        return responseOf(loader.addDerivative(id, word, input, version));
    }

    @POST
    @Path("/remove/part")
    public Response removePart(RemovePartReq q) {
        loader.removePart(q.getWord(), q.getPart(), q.getPath(), q.getAttr(), q.getUserId());
        return responseOf();
    }

    @Data
    public static class RemovePartReq {
        private String word;
        private String part;
        private String path;
        private JsonNode attr;
        private int userId;
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
    @Path("/roots")
    public SetResponse<WordDictLoader.Root> root(@QueryParam("root") String root) {
        return responseOf(loader.roots(root));
    }

    @GET
    @Path("/stat")
    public SingleResponse<WordLoaderCheck> stat(@QueryParam("date") String date,
                                                @QueryParam("userId") int userId) {
        return responseOf(loader.stat(date, userId));
    }

    @GET
    @Path("/stats")
    public ListResponse<WordLoaderCheck> stats(@QueryParam("userId") int userId) {
        return responseOf(loader.stats(userId));
    }

    @GET
    @Path("/dicts")
    public ListResponse<WordDict> dicts(@QueryParam("date") String date) {
        return responseOf(loader.dicts(date));
    }

    @GET
    @Path("/search")
    public ListResponse<WordDict> dict(@QueryParam("q") String q) {
        return responseOf(loader.search(q));
    }

    @GET
    @Path("/affix")
    public SingleResponse<WordLoaderAffix> affix(@QueryParam("word") String word) {
        return responseOf(loader.affix(word));
    }

    @GET
    @Path("/differs")
    public ListResponse<WordDictLoader.Differs> differs(@QueryParam("word") String word) {
        return responseOf(loader.differs(word));
    }

    @GET
    @Path("/trees")
    public ListResponse<WordDictTree> trees(@QueryParam("root") String root) {
        return responseOf(loader.trees(root));
    }

    @GET
    @Path("findTree")
    public ListResponse<WordDictTree> findTree(@QueryParam("word") String word) {
        return responseOf(loader.findTree(word));
    }

    @GET
    @Path("/createTree")
    public Response createTree(@QueryParam("word") String word) {
        loader.createTree(word);
        return responseOf();
    }

    @GET
    @Path("/mergeTree")
    public SingleResponse<WordDictTree> mergeTree(@QueryParam("treeId") String treeId, @QueryParam("word") String word) {
        return responseOf(loader.mergeTree(treeId, word));
    }

    @GET
    @Path("/editTreeDesc")
    public Response editTreeDesc(@QueryParam("treeId") String treeId,
                                 @QueryParam("desc") String desc,
                                 @QueryParam("version") int version) {
        loader.editTreeDesc(treeId, desc, version);
        return responseOf();
    }

    @GET
    @Path("/chat")
    public SingleResponse<String> chat(@QueryParam("q") String q) {
        return responseOf(loader.chat(q));
    }

    @GET
    @Path("/book")
    public Response tag(@QueryParam("path") String path,
                        @QueryParam("tag") String tag,
                        @QueryParam("name") String name) {
        loader.loadBook(path, tag, name);
        return responseOf();
    }
}