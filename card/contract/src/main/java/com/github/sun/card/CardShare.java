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
@Table(name = "card_share")
public class CardShare {
  @Id
  private String id;
  private String shareUserId;
  private String shareId;
  private boolean success;
  @Transient
  private Date createTime;
  @Transient
  private Date updateTime;
}