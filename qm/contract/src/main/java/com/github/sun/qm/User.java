package com.github.sun.qm;

import com.github.sun.foundation.boot.exception.UnexpectedException;
import com.github.sun.foundation.boot.utility.Hex;
import com.github.sun.foundation.modelling.Converter;
import com.github.sun.foundation.modelling.JsonHandler;
import com.github.sun.foundation.modelling.NamingStrategy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamingStrategy
@Table(name = "qm_user")
public class User {
    @Id
    private String id;
    private String username;
    private String nickName;
    private String password;
    private String avatar;
    private String email;
    private BigDecimal amount;
    private boolean vip;
    private Date vipStartTime;
    private Date vipEndTime;
    private Date signInTime;
    private int signInCount;
    private Date lastLoginTime;
    private String lastLoginIp;
    private String location;
    @Converter(JsonHandler.SetStringHandler.class)
    private Set<String> readSystemMessageIds;
    @Transient
    private Date createTime;
    @Transient
    private Date updateTime;

    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] input = password.getBytes(StandardCharsets.UTF_8);
            byte[] output = md.digest(input);
            return Hex.bytes2readable(output);
        } catch (NoSuchAlgorithmException ex) {
            throw new UnexpectedException("呃！出错了");
        }
    }
}
