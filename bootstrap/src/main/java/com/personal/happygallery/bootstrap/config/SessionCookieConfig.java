package com.personal.happygallery.bootstrap.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.session.autoconfigure.DefaultCookieSerializerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SessionCookieConfig {

    @Bean
    DefaultCookieSerializerCustomizer sessionCookieCustomizer(
            @Value("${server.servlet.session.cookie.name}") String name,
            @Value("${server.servlet.session.cookie.http-only}") boolean httpOnly,
            @Value("${server.servlet.session.cookie.same-site}") String sameSite,
            @Value("${server.servlet.session.cookie.secure}") boolean secure) {
        return serializer -> {
            serializer.setCookieName(name);
            serializer.setUseHttpOnlyCookie(httpOnly);
            serializer.setSameSite(sameSite);
            serializer.setUseSecureCookie(secure);
        };
    }
}
