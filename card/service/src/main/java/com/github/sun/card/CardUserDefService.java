package com.github.sun.card;

import com.github.sun.foundation.boot.exception.NotFoundException;
import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.foundation.sql.IdGenerator;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RefreshScope
@RequiredArgsConstructor
public class CardUserDefService {
  private final CardUserDefMapper mapper;
  private final CardUserMapper userMapper;
  private final CardStoreService storeService;

  public CardUserDef byUserId(String userId) {
    CardUserDef def = mapper.byUserId(userId);
    if (def == null) {
      throw new NotFoundException("找不到该用户的卡牌");
    }
    return def;
  }

  @Transactional
  public void edit(String userId, String itemId, String title, String content, String src) {
    CardUserDef value = byUserId(userId);
    List<CardUserDef.Def> defs = value.getDefs();
    CardUserDef.Def def = defs.get(0);
    for (CardUserDef.Item item : def.getItems()) {
      if (Objects.equals(item.getId(), itemId)) {
        item.setTitle(title);
        item.setContent(content);
        item.setSrc(src);
        mapper.update(value);
        break;
      }
    }
  }

  @Transactional
  public void add(String userId, String title, String content, String src) {
    CardUserDef def = byUserId(userId);
    List<CardUserDef.Def> defs = def.getDefs();
    CardUserDef.Item item = new CardUserDef.Item();
    item.setId(IdGenerator.next());
    item.setTitle(title);
    item.setContent(content);
    item.setEnable(true);
    item.setDefaulted(false);
    item.setSrc(src);
    defs.get(0).getItems().add(0, item);
    mapper.update(def);
  }

  @Transactional
  public void delete(String userId, String itemId) {
    CardUserDef value = byUserId(userId);
    List<CardUserDef.Def> defs = value.getDefs();
    CardUserDef.Def def = defs.get(0);
    Iterator<CardUserDef.Item> it = def.getItems().iterator();
    CardUserDef.Item item = null;
    while (it.hasNext()) {
      item = it.next();
      if (Objects.equals(item.getId(), itemId)) {
        it.remove();
        break;
      }
    }
    mapper.update(value);
    if (item != null &&
      !item.isDefaulted() &&
      item.getSrc() != null &&
      !item.getSrc().isEmpty()) {
      storeService.remove(item.getSrc());
    }
  }

  @Transactional
  public void enable(String userId, String itemId, boolean enable) {
    CardUserDef value = byUserId(userId);
    List<CardUserDef.Def> defs = value.getDefs();
    CardUserDef.Def def = defs.get(0);
    for (CardUserDef.Item item : def.getItems()) {
      if (Objects.equals(item.getId(), itemId)) {
        if (!Objects.equals(item.isEnable(), enable)) {
          item.setEnable(enable);
          mapper.update(value);
        }
        break;
      }
    }
  }

  @Transactional
  public void init(String usrId) {
    List<CardC> list = JSON.deserializeAsList(CARDS, CardC.class);
    CardUserDef.Def def = new CardUserDef.Def();
    def.setName("金杯之奕");
    List<CardUserDef.Item> items = new ArrayList<>();
    for (int i = 1; i <= 50; i++) {
      CardUserDef.Item item = new CardUserDef.Item();
      item.setId(IdGenerator.next());
      item.setDefaulted(true);
      item.setTitle(list.get(i - 1).getTitle());
      item.setContent(list.get(i - 1).getContent());
      item.setSrc("/cards/default/" + i + ".png");
      items.add(item);
      item.setEnable(true);
    }
    def.setItems(items);
    CardUserDef v = new CardUserDef();
    v.setId(IdGenerator.next());
    v.setUserId(usrId);
    v.setDefs(Collections.singletonList(def));
    mapper.insert(v);
  }

  @Transactional
  public void fresh() {
    List<CardC> list = JSON.deserializeAsList(CARDS, CardC.class);
    int c = 1;
    for (CardUser v : userMapper.all()) {
      CardUserDef def = mapper.byUserId(v.getId());
      List<CardUserDef.Item> items = def.getDefs().get(0).getItems();
      int i = 0;
      for (CardUserDef.Item item : items) {
        if (item.isDefaulted()) {
          item.setTitle(list.get(i).getTitle());
          item.setContent(list.get(i).getContent());
          i += 1;
        }
      }
      mapper.update(def);
      System.out.println(c++);
    }
  }

  @Data
  public static class CardC {
    private String title;
    private String content;
  }

  private static final String CARDS = "[\n" +
    "  {\n" +
    "    \"title\": \"背水一战\",\n" +
    "    \"content\": \"找指定一名玩家干杯，喝到他举白旗为止\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"变形重组器\",\n" +
    "    \"content\": \"所有人一起唱一首歌举杯\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"不屈意志\",\n" +
    "    \"content\": \"认为在场嘴最硬的人喝一杯\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"超级加倍\",\n" +
    "    \"content\": \"一口气喝完三杯\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"超级加倍\",\n" +
    "    \"content\": \"下一位喝酒的人喝三倍\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"成群结队\",\n" +
    "    \"content\": \"选择任意几个人欺负一个人玩游戏\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"存心失利\",\n" +
    "    \"content\": \"抽到该牌则给下一个玩家发小红包，不低于5.2\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"打起\",\n" +
    "    \"content\": \"自己喝一杯酒其他人必须鼓掌活跃气氛\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"珠光护手\",\n" +
    "    \"content\": \"石头剪刀布，所有出和你一样手势的一起喝一杯\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"恶魔契约\",\n" +
    "    \"content\": \"指定一位玩家签订契约陪每次自己输了喝一杯\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"二进制空投\",\n" +
    "    \"content\": \"从你开始每第二个人空投一杯酒\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"飞升\",\n" +
    "    \"content\": \"自己站起来说一段好听的话喝酒\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"高端购物\",\n" +
    "    \"content\": \"必须用一杯酒购买两次摸牌机会\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"金蛋\",\n" +
    "    \"content\": \"三轮没喝酒孵化收款码，每个人必须给你发红包\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"开摆\",\n" +
    "    \"content\": \"不准喝酒必须选一个人玩真心话大冒险\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"利滚利\",\n" +
    "    \"content\": \"给所有人轮着敬酒\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"联合抵抗\",\n" +
    "    \"content\": \"被其他每个人单挑游戏输了喝酒\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"明智消费\",\n" +
    "    \"content\": \"找人单挑玩游戏，自己定酒杯数别人三倍\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"潘多拉的酒杯\",\n" +
    "    \"content\": \"本轮游戏自己喝什么由自己控制\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"起飞咯\",\n" +
    "    \"content\": \"一起干杯，自己多喝一杯\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"前进之路\",\n" +
    "    \"content\": \"本回合无摸牌机会，被惩罚喝酒只喝一半\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"清晰头脑\",\n" +
    "    \"content\": \"选择一位异性用朋友指定油腻的话表白\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"双重麻烦\",\n" +
    "    \"content\": \"喝一杯酒又选玩真心话或大冒险\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"天涯若比邻\",\n" +
    "    \"content\": \"找离你最远的人干杯你们连喝三杯\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"无敌双刃\",\n" +
    "    \"content\": \"找人玩游戏输了喝两杯\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"掀桌\",\n" +
    "    \"content\": \"蹲在桌子底下喝一杯\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"小小巨人\",\n" +
    "    \"content\": \"把酒换成瓶盖找人干一杯\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"新人入队\",\n" +
    "    \"content\": \"可以拉一名异性入伙每次喝酒异性必须交杯酒陪着喝直到10回合结束\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"药剂师\",\n" +
    "    \"content\": \"可以选给下一个人的酒里加料\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"意外之财\",\n" +
    "    \"content\": \"自罚一杯，意外获得一杯\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"银色赠礼\",\n" +
    "    \"content\": \"指定一位异性给自己必须说自己一个有点\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"羽量级选手\",\n" +
    "    \"content\": \"指定玩家玩快拳/或者快速十五二十\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"战争财宝\",\n" +
    "    \"content\": \"喝完一杯之后和一位异性互换外套直到回合结束\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"直击弱点\",\n" +
    "    \"content\": \"当众念出下一个玩家的个性签名或者最新动态\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"中亚金身\",\n" +
    "    \"content\": \"可以免除一次惩罚\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"珠光莲花\",\n" +
    "    \"content\": \"拿到此牌的人可以爆金币(所有玩家给自己发随机红包)\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"猪脑过载\",\n" +
    "    \"content\": \"举杯共饮自己学猪叫喝下一杯，咽下后再学猪叫\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"不拘一格\",\n" +
    "    \"content\": \"你必须和任意玩家换位置并让他喝一杯\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"成吨的伤害\",\n" +
    "    \"content\": \"让异性们都喝一杯\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"喝酒备战席\",\n" +
    "    \"content\": \"可以任何时候突然让左边的人喝两杯\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"换挡齿轮\",\n" +
    "    \"content\": \"指定两名玩家喝一杯交杯酒\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"禁烟令\",\n" +
    "    \"content\": \"此刻在抽烟的喝一杯\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"静水监狱\",\n" +
    "    \"content\": \"指定一个人立刻去上厕所\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"恋爱脑\",\n" +
    "    \"content\": \"翻出一页最近和暧昧对象的聊天记录给大家看\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"潘多拉的酒杯\",\n" +
    "    \"content\": \"玩一局7的倍数\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"潘多拉的酒杯\",\n" +
    "    \"content\": \"玩一局逛三园(菜园果园动物园)\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"社交强化\",\n" +
    "    \"content\": \"去加在场任意一个人微信，都加过酒自己喝一杯\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"下次一定\",\n" +
    "    \"content\": \"不用喝了，但是你得赶紧收拾东西回家\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"正义之拳\",\n" +
    "    \"content\": \"与你最近的女生十指相扣10回合\"\n" +
    "  },\n" +
    "  {\n" +
    "    \"title\": \"重新来过\",\n" +
    "    \"content\": \"必须喝一杯酒抽一次牌\"\n" +
    "  }\n" +
    "]";
}