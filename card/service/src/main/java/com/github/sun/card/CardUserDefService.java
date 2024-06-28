package com.github.sun.card;

import com.github.sun.foundation.boot.exception.NotFoundException;
import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.foundation.sql.IdGenerator;
import com.ibm.icu.impl.data.ResourceReader;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RefreshScope
@RequiredArgsConstructor
public class CardUserDefService {
  private final CardUserDefMapper mapper;
  private final CardStoreService storeService;

  @Transactional
  public CardUserDef byUserId(String userId) {
    CardUserDef def = mapper.byUserId(userId);
    if (def == null) {
      throw new NotFoundException("找不到该用户的卡牌");
    }
    boolean c = initLove(def, "love99");
    if (c) {
      mapper.update(def);
    }
    return def;
  }

  @Transactional
  public void edit(String userId, String itemId, String title, String content, String src, String cardType) {
    CardUserDef value = byUserId(userId);
    List<CardUserDef.Def> defs = value.getDefs();
    CardUserDef.Def def = defs.stream().filter(v -> Objects.equals(v.getName(), cardType)).findFirst().orElse(null);
    if (def != null) {
      for (CardUserDef.Item item : def.getItems()) {
        if (Objects.equals(item.getId(), itemId)) {
          String prevSrc = item.getSrc();
          item.setTitle(title);
          item.setContent(content);
          item.setSrc(src);
          mapper.update(value);
          if (!Objects.equals(prevSrc, src)) {
            if (!item.isDefaulted() &&
              prevSrc != null &&
              !prevSrc.isEmpty()) {
              storeService.remove(prevSrc);
            }
          }
          break;
        }
      }
    }
  }

  @Transactional
  public void add(String userId, String title, String content, String src, String cardType) {
    CardUserDef def = byUserId(userId);
    List<CardUserDef.Def> defs = def.getDefs();
    CardUserDef.Def _def = defs.stream().filter(v -> Objects.equals(v.getName(), cardType)).findFirst().orElse(null);
    if (_def != null) {
      CardUserDef.Item item = new CardUserDef.Item();
      item.setId(IdGenerator.next());
      item.setTitle(title);
      item.setContent(content);
      item.setEnable(true);
      item.setDefaulted(false);
      item.setSrc(src);
      _def.getItems().add(0, item);
      mapper.update(def);
    }
  }

  @Transactional
  public void delete(String userId, String itemId, String cardType) {
    CardUserDef value = byUserId(userId);
    List<CardUserDef.Def> defs = value.getDefs();
    CardUserDef.Def def = defs.stream().filter(v -> Objects.equals(v.getName(), cardType)).findFirst().orElse(null);
    if (def != null) {
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
  }

  @Transactional
  public void enable(String userId, String itemId, boolean enable, String cardType) {
    CardUserDef value = byUserId(userId);
    List<CardUserDef.Def> defs = value.getDefs();
    CardUserDef.Def def = defs.stream().filter(v -> Objects.equals(v.getName(), cardType)).findFirst().orElse(null);
    if (def != null) {
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
  }

  @Transactional
  public void init(String usrId) {
    CardUserDef def = new CardUserDef();
    def.setId(IdGenerator.next());
    def.setUserId(usrId);
    initHKS(def);
    initLove(def, "love99");
    mapper.insert(def);
  }

  private void initHKS(CardUserDef value) {
    ClassLoader loader = ResourceReader.class.getClassLoader();
    try (InputStream in = loader.getResourceAsStream("cards/hks.json")) {
      if (in != null) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String content = reader.lines().collect(Collectors.joining("\n"));
        List<HKSCard> list = JSON.deserializeAsList(content, HKSCard.class);
        CardUserDef.Def def = new CardUserDef.Def();
        def.setName("hks");
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
        value.getDefs().add(def);
      }
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private boolean initLove(CardUserDef value, String name) {
    if (value.getDefs().stream().noneMatch(v -> Objects.equals(v.getName(), name))) {
      ClassLoader loader = ResourceReader.class.getClassLoader();
      try (InputStream in = loader.getResourceAsStream("cards/" + name + ".json")) {
        if (in != null) {
          BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
          String content = reader.lines().collect(Collectors.joining("\n"));
          LoveCard card = JSON.deserialize(content, LoveCard.class);
          CardUserDef.Def def = new CardUserDef.Def();
          def.setName(name);
          List<CardUserDef.Item> items = new ArrayList<>();
          for (int i = 1; i <= card.cards.size(); i++) {
            LoveCard._C c = card.cards.get(i - 1);
            c.getNames().forEach(n -> {
              CardUserDef.Item item = new CardUserDef.Item();
              item.setId(IdGenerator.next());
              item.setDefaulted(true);
              item.setTitle(c.getType());
              item.setContent(n);
              item.setSrc(c.icon);
              items.add(item);
              item.setEnable(true);
              items.add(item);
            });
          }
          def.setItems(items);
          value.getDefs().add(def);
        }
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
      return true;
    }
    return false;
  }

  @Data
  public static class HKSCard {
    private String title;
    private String content;
  }

  @Data
  public static class LoveCard {
    private String type;
    private String name;
    private List<_C> cards;

    @Data
    public static class _C {
      private String type;
      private String icon;
      private List<String> names;
    }
  }
}