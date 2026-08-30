package com.personal.happygallery.bootstrap.config;

import java.util.Locale;
import org.springframework.boot.session.autoconfigure.DefaultCookieSerializerCustomizer;
import org.springframework.boot.web.server.Cookie;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SessionCookieConfig {

    @Bean
    DefaultCookieSerializerCustomizer sessionCookieCustomizer(ServerProperties serverProperties) {
        Cookie cookie = serverProperties.getServlet().getSession().getCookie();
        return serializer -> {
            serializer.setCookieName(cookie.getName());
            serializer.setUseHttpOnlyCookie(cookie.getHttpOnly());
            serializer.setSameSite(cookie.getSameSite().attributeValue().toLowerCase(Locale.ROOT));
            serializer.setUseSecureCookie(cookie.getSecure());
        };
    }
}
