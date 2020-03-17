package com.github.sun.xfg.resolver;

import com.github.sun.foundation.rest.RequestScopeContextResolver;
import com.github.sun.xfg.User;
import com.github.sun.xfg.UserMapper;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import javax.inject.Inject;

public class MayLoginResolver implements RequestScopeContextResolver<MayLogin> {
  private final Session session;
  private final UserMapper mapper;

  @Inject
  public MayLoginResolver(Session session, UserMapper mapper) {
    this.session = session;
    this.mapper = mapper;
  }

  @Override
  public MayLogin get() {
    String token = session.token;
    if (token == null) {
      return new MayLogin(null);
    }
    User user = mapper.findById(token);
    return new MayLogin(user);
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
          bindFactory(MayLoginResolver.class).to(MayLogin.class);
        }
      };
    }
  }
}
