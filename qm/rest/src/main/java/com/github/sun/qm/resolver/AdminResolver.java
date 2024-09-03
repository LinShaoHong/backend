package com.github.sun.qm.resolver;

import com.github.sun.foundation.boot.exception.UnAuthorizedException;
import com.github.sun.foundation.boot.utility.AES;
import com.github.sun.foundation.rest.RequestScopeContextResolver;
import com.github.sun.qm.admin.Admin;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.springframework.core.env.Environment;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Cookie;

import static com.github.sun.qm.admin.Admin.ADMIN_TOKEN_NAME;

public class AdminResolver implements RequestScopeContextResolver<Admin> {
    private final String username;
    private final String password;
    private final String secretKey;
    private final ContainerRequestContext request;

    @Inject
    public AdminResolver(Environment env, ContainerRequestContext request) {
        this.request = request;
        this.username = env.getProperty("admin.username");
        this.password = env.getProperty("admin.password");
        this.secretKey = env.getProperty("base64.secret.key");
    }

    @Override
    public Admin get() {
        String token = request.getUriInfo().getQueryParameters().getFirst(ADMIN_TOKEN_NAME);
        if (token == null) {
            token = request.getHeaderString(ADMIN_TOKEN_NAME);
            if (token == null) {
                Cookie cookie = request.getCookies().get(ADMIN_TOKEN_NAME);
                if (cookie != null) {
                    token = cookie.getValue();
                }
            }
        }
        if (token == null) {
            StringBuilder sb = new StringBuilder();
            request.getHeaders().forEach((k, v) -> sb.append(k).append("=").append(String.join(",", v)).append("&"));
            throw new UnAuthorizedException("UnAuthorized: Missing Token For Request=" + sb);
        }
        String id = AES.decrypt(token, secretKey);
        String[] arr = id.split(":");
        if (arr.length > 1 && username.equals(arr[0]) && password.equals(arr[1])) {
            return new Admin(arr[0]);
        }
        throw new UnAuthorizedException("UnAuthorized: Wrong Input For DecodeToken=" + id);
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
                    bindFactory(AdminResolver.class).to(Admin.class);
                }
            };
        }
    }
}
