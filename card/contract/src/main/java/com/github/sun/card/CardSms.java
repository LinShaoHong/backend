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
import java.util.Objects;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "card_sms")
public class CardSms {
    @Id
    private String id;
    private String userId;
    private String fromPhone;
    private String toPhone;
    private String message;
    private Date time;
    @Transient
    private Date createTime;
    @Transient
    private Date updateTime;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof CardSms))
            return false;
        CardSms cardSms = (CardSms) o;
        return getId().equals(cardSms.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}