package com.github.sun.qm.resolver;

import com.github.sun.foundation.rest.RequestScopeContextResolver;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Cookie;

import static com.github.sun.qm.SessionService.TOKEN_NAME;

public class SessionResolver implements RequestScopeContextResolver<Session> {
  private final ContainerRequestContext request;

  @Inject
  public SessionResolver(ContainerRequestContext request) {
    this.request = request;
  }

  @Override
  public Session get() {
    String token = request.getUriInfo().getQueryParameters().getFirst(TOKEN_NAME);
    if (token == null) {
      token = request.getHeaderString(TOKEN_NAME);
      if (token == null) {
        Cookie cookie = request.getCookies().get(TOKEN_NAME);
        if (cookie != null) {
          token = cookie.getValue();
        }
      }
    }
    return new Session(token);
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
          bindFactory(SessionResolver.class).to(Session.class);
        }
      };
    }
  }
}
