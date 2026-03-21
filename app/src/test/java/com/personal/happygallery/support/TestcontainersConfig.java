package com.personal.happygallery.support;

import com.personal.happygallery.common.time.Clocks;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

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
    @ServiceConnection
    MySQLContainer mysqlContainer() {
        return new MySQLContainer("mysql:8.0");
    }

    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379);
    }
}
