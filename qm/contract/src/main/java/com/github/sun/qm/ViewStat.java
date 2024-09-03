package com.github.sun.qm;

import com.github.sun.foundation.modelling.NamingStrategy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "qm_view_stat")
public class ViewStat {
    @Id
    private String id;
    private Girl.Type type;
    private String date;
    private long visits;

    public static String makeId(Girl.Type type, String city, String date) {
        if (city != null && !city.isEmpty()) {
            return type.name() + ":" + city + ":" + date;
        }
        return type.name() + ":TOTAL:" + date;
    }
}
