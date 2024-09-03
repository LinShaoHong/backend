package com.github.sun.qm.resolver;

import com.github.sun.foundation.boot.utility.AES;
import com.github.sun.foundation.rest.RequestScopeContextResolver;
import com.github.sun.qm.User;
import com.github.sun.qm.UserMapper;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.springframework.core.env.Environment;

import javax.inject.Inject;

public class MayLoginResolver implements RequestScopeContextResolver<MayLogin> {
    private final String secretKey;
    private final Session session;
    private final UserMapper mapper;

    @Inject
    public MayLoginResolver(Environment env, Session session, UserMapper mapper) {
        this.secretKey = env.getProperty("base64.secret.key");
        this.session = session;
        this.mapper = mapper;
    }

    @Override
    public MayLogin get() {
        String token = session.token;
        if (token == null) {
            return new MayLogin(null);
        }
        String userId = AES.decrypt(token, secretKey);
        User user = mapper.findById(userId);
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
