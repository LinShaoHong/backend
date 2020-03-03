package com.github.sun.mall.admin;

import com.github.sun.foundation.boot.exception.UnAuthorizedException;
import com.github.sun.foundation.rest.RequestScopeContextResolver;
import com.github.sun.mall.admin.entity.Admin;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Cookie;

import static com.github.sun.mall.admin.AdminSessionService.TOKEN_NAME;

public class AdminSessionResolver implements RequestScopeContextResolver<Admin> {
  private final AdminMapper mapper;
  private final ContainerRequestContext request;

  @Inject
  public AdminSessionResolver(AdminMapper mapper, ContainerRequestContext request) {
    this.mapper = mapper;
    this.request = request;
  }

  @Override
  public Admin get() {
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
    if (token == null) {
      throw new UnAuthorizedException("请先登录");
    }
    Admin admin = mapper.findById(token);
    if (admin == null) {
      throw new UnAuthorizedException("用户不存在，请先登录");
    }
    return admin;
  }

  @Override
  public void remove() {
  }

  public static class BinderProviderImpl extends AbstractProvider<AdminSessionResolver, Admin> {
  }
}

