package com.personal.happygallery.support;

import com.personal.happygallery.domain.time.Clocks;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistrar;

import java.time.Clock;
import java.time.ZonedDateTime;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfig {

    @Bean
    @Primary
    Clock fixedClock() {
        return Clock.fixed(
                ZonedDateTime.of(2026, 3, 1, 10, 0, 0, 0, Clocks.SEOUL).toInstant(),
                Clocks.SEOUL);
    }

    @Bean
    DynamicPropertyRegistrar testConnections() {
        var mysql = SharedTestContainers.newDatabase();
        var redis = SharedTestContainers.newRedisDatabase();
        return registry -> {
            registry.add("spring.datasource.url", mysql::jdbcUrl);
            registry.add("spring.datasource.username", mysql::username);
            registry.add("spring.datasource.password", mysql::password);
            registry.add("spring.data.redis.host", redis::host);
            registry.add("spring.data.redis.port", redis::port);
            registry.add("spring.data.redis.database", redis::database);
        };
    }
}
