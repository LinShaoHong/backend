package com.github.sun.card;

import com.github.sun.foundation.sql.IdGenerator;
import lombok.*;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RefreshScope
@RequiredArgsConstructor
public class CardSmsService {
  private final CardSmsMapper mapper;
  private final CardUserMapper userMapper;
  private final CardSmsSpecMapper specMapper;

  public List<String> getSpecsByType(String type) {
    return specMapper.byType(type);
  }

  @Transactional
  public void send(String userId, String fromPhone, String toPhone, String message) {
    CardUser user = userMapper.findById(userId);
    if (user != null) {
      CardSms sms = CardSms.builder()
        .id(IdGenerator.next())
        .userId(userId)
        .fromPhone(fromPhone)
        .toPhone(toPhone)
        .message(message)
        .time(new Date())
        .build();
      if (user.getSmsCount() > 0) {
        user.setSmsCount(user.getSmsCount() - 1);
        userMapper.update(user);
      }
      mapper.insert(sms);
    }
  }

  public List<Record> records(String userId) {
    CardUser user = userMapper.findById(userId);
    if (user != null) {
      Set<CardSms> sms = new HashSet<>(mapper.byUserId(userId));
      if (StringUtils.hasText(user.getPhone())) {
        sms.addAll(mapper.byToPhone(user.getPhone()));
      }
      List<CardSms> list = new ArrayList<>(sms);
      list.sort(Comparator.comparing(CardSms::getTime));
      Set<String> visitor = new HashSet<>();
      List<Record> ret = new ArrayList<>();
      SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      list.forEach(v -> {
        String phone;
        if (Objects.equals(v.getUserId(), userId)) {
          phone = v.getToPhone();
        } else {
          phone = v.getFromPhone();
        }
        if (phone != null) {
          if (!visitor.contains(phone)) {
            ret.add(Record.builder()
              .phone(phone)
              .time(format.format(v.getTime()))
              .type(Objects.equals(v.getUserId(), userId) ? "send" : "receive")
              .build());
          }
          visitor.add(phone);
        }
      });
      return ret;
    }
    return Collections.emptyList();
  }

  public List<Chat> chats(String userId, String phone) {
    CardUser user = userMapper.findById(userId);
    if (user != null) {
      Set<CardSms> sms = new HashSet<>(mapper.byUserId(userId));
      sms.removeIf(v -> !Objects.equals(v.getToPhone(), phone));
      if (StringUtils.hasText(user.getPhone())) {
        sms.addAll(mapper.byFromPhoneAndToPhone(phone, user.getPhone()));
      }
      List<CardSms> list = new ArrayList<>(sms);
      list.sort(Comparator.comparing(CardSms::getTime));
      SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      return list.stream()
        .map(v ->
          Chat.builder()
            .id(v.getId())
            .userId(v.getUserId())
            .message(v.getMessage())
            .time(format.format(v.getTime()))
            .build())
        .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  public List<String> recPhones(String userId) {
    return records(userId).stream().map(Record::getPhone).distinct().collect(Collectors.toList());
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Chat {
    private String id;
    private String message;
    private String userId;
    private String time;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Record {
    private String phone;
    private String time;
    private String type;
  }
}