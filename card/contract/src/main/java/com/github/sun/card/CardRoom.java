package com.github.sun.card;

import com.github.sun.foundation.modelling.NamingStrategy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "card_room")
public class CardRoom {
  @Id
  private String id;
  private String mainUserId;
  private String userId;
  private Date enterTime;
  @Transient
  private Date createTime;
  @Transient
  private Date updateTime;
}