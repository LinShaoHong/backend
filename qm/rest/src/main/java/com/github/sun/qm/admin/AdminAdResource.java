package com.github.sun.qm.admin;

import com.github.sun.foundation.rest.AbstractResource;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Random;

@Path("/v1/qm/admin/ad")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AdminAdResource extends AbstractResource {
  /**
   * 随机获取英文短句文案
   */
  @GET
  @Path("/gen/sentence")
  public SingleResponse<String> genSentence(@Context Admin admin) {
    String[] arr = {
      "The sound of birds singing in the morning was like a symphony of nature.",
      "The smell of fresh coffee brewing in the morning was like a comforting hug.",
      "The sight of a rainbow after a rainstorm was like a promise of hope.",
      "The feel of sand under your feet at the beach was like a massage for your soul.",
      "The taste of a ripe peach on a hot summer day was like a burst of sunshine in your mouth.",
      "The sound of a waterfall cascading down a mountain was like a symphony of peace.",
      "The sight of a full moon rising over the horizon was like a magical moment frozen in time.",
      "The feel of a cool breeze on a hot day was like a refreshing drink for your skin.",
      "The smell of a campfire in the evening was like a warm embrace from nature.",
      "The taste of a perfectly ripe strawberry was like a burst of sweetness in your mouth.",
      "The sight of a field of wildflowers was like a canvas painted with nature's colors.",
      "The sound of a crackling fire in the fireplace was like a cozy blanket for your ears.",
      "The feel of warm sand between your toes at the beach was like a massage for your soul.",
      "The taste of a freshly baked chocolate chip cookie was like a warm hug from your grandma.",
      "The smell of freshly cut grass was like a reminder of lazy summer days.",
      "The sight of a mountain peak rising above the clouds was like a symbol of strength and perseverance.",
      "The sound of a choir singing in harmony was like a heavenly chorus.",
      "The feel of a soft breeze on your skin was like a gentle caress from a loved one.",
      "The taste of a perfectly cooked steak was like a celebration of life's simple pleasures.",
      "The smell of a rose garden in full bloom was like a reminder of the beauty of life.",
      "The sight of a city skyline lit up at night was like a dazzling display of human achievement.",
      "The sound of waves crashing against the shore was like a reminder of the power of nature.",
      "The feel of a warm hug from a loved one was like a balm for the soul.",
      "The taste of a juicy watermelon on a hot summer day was like a refreshing oasis in the desert.",
      "The smell of freshly baked bread was like a hug from your mom.",
      "The sight of a butterfly emerging from its cocoon was like a reminder of the beauty of transformation.",
      "The sound of a babbling brook in the forest was like a soothing melody.",
      "The feel of a warm summer rain on your skin was like a dance with nature.",
      "The taste of a perfectly ripe mango was like a burst of tropical sunshine in your mouth.",
      "The smell of a pine forest in the morning was like a reminder of the simple joys of life.",
      "The sight of a hummingbird in flight was like a symbol of grace and agility.",
      "The sound of a symphony orchestra playing a Beethoven masterpiece was like a journey to another world.",
      "The feel of soft sand under your feet at the beach was like a massage for your soul.",
      "The taste of a freshly squeezed lemonade was like a burst of sour sweetness in your mouth.",
      "The smell of a freshly brewed cup of tea was like a comforting hug from your grandma.",
      "The sight of a colorful sunset over the ocean was like a painting by a master artist.",
      "The sound of a thunderstorm in the distance was like a warning of the power of nature.",
      "The feel of a soft blanket on a cold winter night was like a cozy hug from your loved one.",
      "The taste of a perfectly cooked piece of salmon was like a celebration of the ocean's bounty.",
      "The smell of a blooming lavender field was like a reminder of the beauty of nature.",
      "The sight of a full moon reflected on a calm lake was like a moment of tranquility.",
      "The sound of a saxophone playing a soulful melody was like a journey to the heart of music.",
      "The feel of a cool breeze on a hot summer day was like a refreshing drink for your soul.",
      "The taste of a freshly baked apple pie was like a warm hug from your mom.",
      "The smell of a freshly cut cedar tree was like a reminder of the majesty of nature.",
      "The sight of a waterfall at the end of a hike was like a reward for perseverance.",
      "The sound of a choir singing a hymn was like a reminder of the beauty of faith.",
      "The feel of a warm cup of cocoa on a cold winter day was like a comforting hug from your loved one.",
      "The taste of a perfectly brewed cup of coffee was like a kickstart to your day.",
      "The smell of a campfire on a summer evening was like a reminder of childhood memories.",
      "The sight of a field of sunflowers was like a celebration of the sun's warmth and light.",
      "The sound of a piano playing a Chopin nocturne was like a journey to the soul of music.",
      "The feel of a soft pillow under your head at night was like a comforting embrace.",
      "The taste of a perfectly cooked risotto was like a celebration of the art of cooking.",
      "The smell of a blooming jasmine vine was like a reminder of the beauty of simplicity.",
      "The sight of a flock of birds in flight was like a symbol of freedom.",
      "The sound of a harp playing a classical piece was like a journey to the world of dreams.",
      "The feel of a warm bath after a long day was like a rejuvenating spa treatment.",
      "The taste of a perfectly grilled burger was like a celebration of summer barbecues.",
      "The smell of a rosemary plant in a garden was like a reminder of the beauty of herbs.",
      "The sight of a snow-covered mountain peak was like a symbol of purity and beauty.",
      "The sound of a violin playing a Vivaldi concerto was like a journey to the heart of music.",
      "The feel of a soft scarf around your neck on a cold day was like a warm hug from your loved one.",
      "The taste of a perfectly cooked steak with red wine sauce was like a celebration of a romantic dinner.",
      "The smell of a blooming lilac bush was like a reminder of the beauty of spring.",
      "The sight of a majestic eagle soaring in the sky was like a symbol of power and freedom.",
      "The sound of a trumpet playing a jazz tune was like a journey to the heart of music.",
      "The feel of a soft blanket wrapped around your shoulders on a chilly evening was like a comforting embrace.",
      "The taste of a perfectly roasted chicken with garlic and herbs was like a celebration of family dinners.",
      "The smell of a freshly baked cinnamon roll was like a reminder of the joy of baking.",
      "The sight of a rainbow over a waterfall was like a moment of enchantment.",
      "The sound of a cello playing a Bach suite was like a journey to the soul of music.",
      "The feel of a warm fire on a cold night was like a cozy hug from nature.",
      "The taste of a perfectly cooked rack of lamb with rosemary and garlic was like a celebration of fine dining.",
      "The smell of a blooming cherry blossom tree was like a reminder of the beauty of nature's cycles.",
      "The sight of a sailboat on a calm sea was like a symbol of peace and serenity.",
      "The sound of a clarinet playing a Mozart sonata was like a journey to the heart of music.",
      "The feel of a warm towel after a shower was like a comforting embrace from your loved one.",
      "The taste of a perfectly baked apple crisp was like a celebration of fall flavors.",
      "The smell of a freshly cut cucumber was like a reminder of the beauty of simplicity.",
      "The sight of a field of lavender in Provence was like a moment of bliss.",
      "The sound of a flute playing a Debussy piece was like a journey to the soul of music.",
      "The feel of a soft sweater on a chilly day was like a comforting hug from your loved one.",
      "The taste of a perfectly cooked lobster with butter sauce was like a celebration of seafood.",
      "The smell of a blooming peony bush was like a reminder of the beauty of femininity.",
      "The sight of a starry night sky in the desert was like a moment of awe.",
      "The sound of a guitar playing a folk song was like a journey to the heart of music.",
      "The feel of a soft bed after a long day was like a comforting embrace from your loved one.",
      "The taste of a perfectly cooked filet mignon with red wine reduction was like a celebration of fine dining.",
      "The smell of a blooming magnolia tree was like a reminder of the beauty of the South.",
      "The sight of a field of poppies in Tuscany was like a moment of enchantment."};
    return responseOf(arr[new Random().nextInt(arr.length)]);
  }
}
