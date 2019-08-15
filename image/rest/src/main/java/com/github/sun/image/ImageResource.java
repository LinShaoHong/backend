package com.github.sun.image;

import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.image.mapper.ImageDetailsMapper;
import com.github.sun.image.mapper.ImageMapper;
import com.hankcs.hanlp.HanLP;
import lombok.Builder;
import lombok.Data;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.sun.foundation.expression.Expression.EMPTY;
import static com.github.sun.foundation.expression.Expression.nonNull;

@Path("/api/v1/images")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ImageResource extends AbstractResource {
  private final ImageMapper mapper;
  private final ImageDetailsMapper detailsMapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public ImageResource(ImageMapper mapper,
                       ImageDetailsMapper detailsMapper,
                       @Named("mysql") SqlBuilder.Factory factory) {
    this.mapper = mapper;
    this.detailsMapper = detailsMapper;
    this.factory = factory;
  }

  @GET
  public PageResponse<ImageResp> getPage(@QueryParam("start") int start,
                                         @QueryParam("count") int count,
                                         @NotNull @QueryParam("type") String type,
                                         @QueryParam("category") String category) {
    SqlBuilder sb = factory.create();
    Expression condition = sb.field("type").eq(type)
      .and(nonNull(category).then(sb.field("categorySpell").contains(category)));
    SqlBuilder.Template template = sb.from(Image.class)
      .where(condition).count().template();
    int total = mapper.countByTemplate(template);
    sb.clear();
    template = sb.from(Image.class)
      .where(condition)
      .desc("visits")
      .limit(start, count)
      .template();
    List<Image> images = mapper.findByTemplate(template);
    return responseOf(total, images.stream()
      .map(ImageResp::from)
      .collect(Collectors.toList()));
  }

  @Data
  @Builder
  public static class ImageResp {
    private String id;
    private String title;
    private String src;
    private long visits;
    private long likes;
    private String[] tags;

    private static ImageResp from(Image v) {
      return ImageResp.builder()
        .id(v.getId())
        .title(v.getTitle())
        .src(v.getLocalPath().substring(Constants.PATH.length()))
        .visits(v.getVisits())
        .likes(v.getLikes())
        .tags(v.getCategory().split("\\|"))
        .build();
    }
  }

  @GET
  @Path("/details/{imgId}")
  public ListResponse<String> getDetails(@PathParam("imgId") String imgId) {
    SqlBuilder sb = factory.create();
    SqlBuilder.Template template = sb.from(Image.class)
      .where(sb.field("id").eq(imgId))
      .update()
      .set("visits", sb.field("visits").plus(1))
      .template();
    mapper.updateByTemplate(template);
    List<Image.Detail> details = detailsMapper.findByImgId(imgId);
    return responseOf(details.stream()
      .map(v -> v.getLocalPath().substring(Constants.PATH.length()))
      .collect(Collectors.toList())
    );
  }

  @PUT
  @Path("/like/{imgId}")
  public void like(@PathParam("imgId") String imgId,
                   @QueryParam("like") boolean like) {
    SqlBuilder sb = factory.create();
    SqlBuilder.Template template = sb.from(Image.class)
      .where(sb.field("id").eq(imgId))
      .update()
      .set("likes", like ? sb.field("likes").plus(1) : sb.field("likes").sub(1))
      .template();
    mapper.updateByTemplate(template);
  }

  @GET
  @Path("/recommendation/{imgId}")
  public ListResponse<ImageResp> get(@PathParam("imgId") String imgId,
                                     @QueryParam("count") int count) {
    Image image = mapper.findById(imgId);
    String category = image.getCategorySpell();
    if (category != null) {
      SqlBuilder sb = factory.create();
      Expression expr = EMPTY;
      for (String sub : category.split("\\|")) {
        Expression e = sb.field("categorySpell").contains(sub);
        expr = expr == EMPTY ? e : expr.or(e);
      }
      SqlBuilder.Template template = sb.from(Image.class)
        .where(expr.and(sb.field("id").ne(imgId)))
        .desc("visits")
        .limit(count)
        .template();
      List<Image> images = mapper.findByTemplate(template);
      return responseOf(images.stream().map(ImageResp::from).collect(Collectors.toList()));
    }
    return responseOf(Collections.emptyList());
  }

  @GET
  @Path("/search")
  public SingleResponse<SearchResp> search(@NotNull @QueryParam("q") String q) {
    if (q.trim().isEmpty()) {
      return responseOf(SearchResp.builder()
        .images(Collections.emptyList())
        .keyWords(Collections.emptyList())
        .build());
    }
    List<String> keyWords = HanLP.extractKeyword(q, 10);
    if (keyWords.isEmpty()) {
      keyWords.add(q);
    }
    SqlBuilder sb = factory.create();
    Expression conn = EMPTY;
    for (String word : keyWords) {
      Expression e = sb.field("title").contains(word);
      conn = conn == EMPTY ? e : conn.or(e);
    }
    SqlBuilder.Template template = sb.from(Image.class)
      .where(conn)
      .desc("visits")
      .template();
    List<Image> images = mapper.findByTemplate(template);
    return responseOf(SearchResp.builder()
      .images(images.stream().map(ImageResp::from).collect(Collectors.toList()))
      .keyWords(keyWords)
      .build());
  }

  @Data
  @Builder
  public static class SearchResp {
    private List<ImageResp> images;
    private List<String> keyWords;
  }
}
