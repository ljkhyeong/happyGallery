package com.personal.happygallery.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * Redis 관련 Bean 구성.
 *
 * <p>Spring Data Redis 자동 구성(StringRedisTemplate 등)은 application.yml의
 * spring.data.redis.* 설정을 통해 자동으로 이루어진다.
 *
 * <p>{@link DefaultCookieSerializer}를 통해 Spring Session 쿠키 이름을 {@code HG_SESSION}으로
 * 유지하고 보안 속성(HttpOnly, SameSite=Lax)을 설정한다.
 */
@Configuration
public class RedisConfig {

    @Bean
    DefaultCookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("HG_SESSION");
        serializer.setUseHttpOnlyCookie(true);
        serializer.setSameSite("Lax");
        return serializer;
    }
}
