package com.github.sun.xzyy.resolver;

import com.github.sun.foundation.boot.exception.UnAuthorizedException;
import com.github.sun.foundation.rest.RequestScopeContextResolver;
import com.github.sun.xzyy.User;
import com.github.sun.xzyy.UserMapper;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import javax.inject.Inject;

public class UserResolver implements RequestScopeContextResolver<User> {
  private final Session session;
  private final UserMapper mapper;

  @Inject
  public UserResolver(Session session, UserMapper mapper) {
    this.session = session;
    this.mapper = mapper;
  }

  @Override
  public User get() {
    String token = session.token;
    if (token == null) {
      throw new UnAuthorizedException("请先登录");
    }
    User user = mapper.findById(token);
    if (user == null) {
      throw new UnAuthorizedException(1000);
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
