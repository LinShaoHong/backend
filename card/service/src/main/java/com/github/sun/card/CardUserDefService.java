package com.github.sun.card;

import com.github.sun.foundation.boot.exception.NotFoundException;
import com.github.sun.foundation.sql.IdGenerator;
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

  public CardUserDef byUserId(String userId) {
    CardUserDef def = mapper.byUserId(userId);
    if (def == null) {
      throw new NotFoundException("找不到该用户的卡牌");
    }
    return def;
  }

  @Transactional
  public void edit(String userId, String itemId, String title, String content,String picPath) {
    CardUserDef value = byUserId(userId);
    List<CardUserDef.Def> defs = value.getDefs();
    CardUserDef.Def def = defs.get(0);
    for (CardUserDef.Item item : def.getItems()) {
      if (Objects.equals(item.getId(), itemId)) {
        item.setTitle(title);
        item.setContent(content);
        item.setPicPath(picPath);
        mapper.update(value);
        break;
      }
    }
  }

  @Transactional
  public void add(String userId, String title, String content,String picPath) {
    CardUserDef def = byUserId(userId);
    List<CardUserDef.Def> defs = def.getDefs();
    CardUserDef.Item item = new CardUserDef.Item();
    item.setId(IdGenerator.next());
    item.setTitle(title);
    item.setContent(content);
    item.setPicPath(picPath);
    item.setEnable(true);
    item.setDefaulted(false);
    item.setSrc(null);
    defs.get(0).getItems().add(0, item);
    mapper.update(def);
  }

  @Transactional
  public void delete(String userId, String itemId) {
    CardUserDef value = byUserId(userId);
    List<CardUserDef.Def> defs = value.getDefs();
    CardUserDef.Def def = defs.get(0);
    Iterator<CardUserDef.Item> it = def.getItems().iterator();
    while (it.hasNext()) {
      CardUserDef.Item item = it.next();
      if (Objects.equals(item.getId(), itemId)) {
        it.remove();
        break;
      }
    }
    mapper.update(value);
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
    CardUserDef.Def def = new CardUserDef.Def();
    def.setName("金杯之奕");
    List<CardUserDef.Item> items = new ArrayList<>();
    for (int i = 1; i <= 50; i++) {
      CardUserDef.Item item = new CardUserDef.Item();
      item.setId(IdGenerator.next());
      item.setDefaulted(true);
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
}