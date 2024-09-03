package com.github.sun.qm;

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
@Table(name = "qm_footprint")
public class Footprint {
    @Id
    private String id;
    private String userId;
    private String girlId;
    @Transient
    private Date createTime;
    @Transient
    private Date updateTime;

    public static String makeId(String userId, String girlId) {
        return userId + ":" + girlId;
    }
}
