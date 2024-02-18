package com.github.sun.card;

import com.github.sun.foundation.sql.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RefreshScope
@RequiredArgsConstructor
public class CardShareService {
  private final CardShareMapper mapper;

  @Transactional
  public void share(String shareUserId, String shareId) {
    CardShare share = CardShare.builder()
      .id(IdGenerator.next())
      .shareUserId(shareUserId)
      .shareId(shareId)
      .success(false)
      .build();
    mapper.insert(share);
  }
}