package com.github.sun.card.event;

import com.github.sun.card.CardUserDef;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class RoomEvent {
  private String mainUserId;
  private String userId;
  private boolean hks;

  public abstract String getName();

  @Data
  @SuperBuilder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static class ShuffleEvent extends RoomEvent { //洗牌
    private int total;
    private @Builder.Default String name = "ShuffleEvent";
  }

  @Data
  @SuperBuilder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static class OpenEvent extends RoomEvent { //开牌
    private int index;
    private boolean music;
    private CardUserDef.Item item;
    private @Builder.Default String name = "OpenEvent";
  }

  @Data
  @SuperBuilder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static class CloseEvent extends RoomEvent { //关闭
    private @Builder.Default String name = "CloseEvent";
  }

  @Data
  @SuperBuilder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static class AddEvent extends RoomEvent { //邀请加入
    private int avatar;
    private int vip;
    private String nickname;
    private @Builder.Default String name = "AddEvent";
  }

  @Data
  @SuperBuilder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static class NextEvent extends RoomEvent { // 下一个
    private int avatar;
    private int vip;
    private String nickname;
    private @Builder.Default String name = "NextEvent";
  }

  @Data
  @SuperBuilder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static class LeaveEvent extends RoomEvent { //离开
    private @Builder.Default String name = "LeaveEvent";
  }

  @Data
  @SuperBuilder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static class ChangeCardTypeEvent extends RoomEvent { //更改卡片类型
    private String cardType;
    private @Builder.Default String name = "ChangeCardTypeEvent";
  }

  @Data
  @SuperBuilder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static class ReceiveReplyEvent extends RoomEvent { //获取回复
    private String chatId;
    private boolean withdraw;
    private @Builder.Default String name = "ReceiveReplyEvent";
  }
}