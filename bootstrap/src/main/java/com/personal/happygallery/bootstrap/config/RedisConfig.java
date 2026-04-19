package com.personal.happygallery.bootstrap.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.FlushMode;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
@EnableRedisHttpSession(
        maxInactiveIntervalInSeconds = 7 * 24 * 60 * 60,
        redisNamespace = "hg:session",
        flushMode = FlushMode.ON_SAVE)
public class RedisConfig {

    public static final String COOKIE_NAME = "HG_SESSION";

    @Bean
    DefaultCookieSerializer cookieSerializer(
            @Value("${app.session.secure-cookie:true}") boolean secureCookie) {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName(COOKIE_NAME);
        serializer.setUseHttpOnlyCookie(true);
        serializer.setSameSite("Lax");
        serializer.setUseSecureCookie(secureCookie);
        return serializer;
    }
}
