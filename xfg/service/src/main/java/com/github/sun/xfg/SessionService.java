package com.github.sun.xfg;

import com.github.sun.foundation.boot.exception.Message;
import com.github.sun.foundation.sql.IdGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Date;

@Service
public class SessionService {
  public static final String TOKEN_NAME = "XFG-TOKEN";

  @Resource
  private UserMapper mapper;
  @Resource
  private CommentMapper commentMapper;

  @Transactional
  public String register(String username, String password, String email, String ip) {
    if (mapper.countByUsername(username) > 0) {
      throw new Message(1001);
    }
    String id = IdGenerator.next();
    User user = User.builder()
      .id(id)
      .username(username)
      .password(User.hashPassword(password))
      .email(email)
      .lastLoginIp(ip)
      .lastLoginTime(new Date())
      .readSystemMessageIds(Collections.emptySet())
      .build();
    mapper.insert(user);
    return id;
  }

  @Transactional
  public String login(String username, String password, String ip) {
    String hashPassword = User.hashPassword(password);
    User user = mapper.findByUsernameAndPassword(username, hashPassword);
    if (user == null) {
      if (mapper.countByUsername(username) > 0) {
        throw new Message(1002);
      } else {
        throw new Message(1000);
      }
    }
    if (ip != null) {
      user.setLastLoginIp(ip);
    }
    user.setLastLoginTime(new Date());
    mapper.update(user);
    return user.getId();
  }
}
