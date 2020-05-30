package com.github.sun.qm.resolver;

import com.github.sun.foundation.boot.exception.UnAuthorizedException;
import com.github.sun.foundation.boot.utility.AES;
import com.github.sun.foundation.rest.RequestScopeContextResolver;
import com.github.sun.qm.User;
import com.github.sun.qm.UserMapper;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.springframework.core.env.Environment;

import javax.inject.Inject;
import java.util.Calendar;
import java.util.Date;

public class UserResolver implements RequestScopeContextResolver<User> {
  private final Session session;
  private final String secretKey;
  private final UserMapper mapper;

  @Inject
  public UserResolver(Environment env, Session session, UserMapper mapper) {
    this.secretKey = env.getProperty("base64.secret.key");
    this.session = session;
    this.mapper = mapper;
  }

  @Override
  public User get() {
    String token = session.token;
    if (token == null) {
      throw new UnAuthorizedException("请先登录");
    }
    String userId = AES.decrypt(token, secretKey);
    User user = mapper.findById(userId);
    if (user == null) {
      throw new UnAuthorizedException(1000);
    }
    if (user.isVip()) {
      Calendar c = Calendar.getInstance();
      c.setTime(user.getVipEndTime());
      if (c.getTime().compareTo(new Date()) < 0) {
        user.setVip(false);
        mapper.update(user);
      }
    }
    return user;
  }

  @Override
  public void remove() {
  }

  public static class BinderProviderImpl implements BinderProvider {
    @Override
    public AbstractBinder binder() {
      return new AbstractBinder() {
        @Override
        protected void configure() {
          bindFactory(UserResolver.class).to(User.class);
        }
      };
    }
  }
}
