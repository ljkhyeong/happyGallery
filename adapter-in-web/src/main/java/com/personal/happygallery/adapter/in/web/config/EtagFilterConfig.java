package com.personal.happygallery.bootstrap.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

@Configuration
public class EtagFilterConfig {

    @Bean
    FilterRegistrationBean<ShallowEtagHeaderFilter> shallowEtagHeaderFilter() {
        var registration = new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
        registration.addUrlPatterns(
                "/api/v1/products/*", "/api/v1/classes/*", "/api/v1/notices/*",
                "/products/*", "/classes/*", "/notices/*"
        );
        registration.setOrder(0);
        return registration;
    }
}
