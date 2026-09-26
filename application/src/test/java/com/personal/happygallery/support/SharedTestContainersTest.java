package com.personal.happygallery.support;

import java.sql.DriverManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class SharedTestContainersTest {
    @Test
    @DisplayName("같은 MySQL 서버를 재사용해도 테스트 DB의 테이블과 데이터는 격리된다")
    void mysqlServerIsSharedButSchemasAreIsolated() throws Exception {
        var first = SharedTestContainers.newDatabase();
        var second = SharedTestContainers.newDatabase();
        String serverId;
        try (var connection = DriverManager.getConnection(first.jdbcUrl(), first.username(), first.password());
             var statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE isolation_probe (id INT PRIMARY KEY)");
            statement.executeUpdate("INSERT INTO isolation_probe VALUES (1)");
            try (var result = statement.executeQuery("SELECT @@server_uuid")) {
                assertThat(result.next()).isTrue();
                serverId = result.getString(1);
            }
        }
        try (var connection = DriverManager.getConnection(second.jdbcUrl(), second.username(), second.password());
             var statement = connection.createStatement()) {
            try (var result = statement.executeQuery("SELECT @@server_uuid")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString(1)).isEqualTo(serverId);
            }
            try (var result = statement.executeQuery("""
                    SELECT COUNT(*) FROM information_schema.tables
                    WHERE table_schema = DATABASE() AND table_name = 'isolation_probe'
                    """)) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isZero();
            }
        }
    }

    @Test
    @DisplayName("같은 Redis 서버에서도 같은 이름의 키와 연결 종료가 다른 테스트 DB에 영향을 주지 않는다")
    void redisDatabasesAndClientLifecyclesAreIsolated() {
        var first = SharedTestContainers.newRedisDatabase();
        var second = SharedTestContainers.newRedisDatabase();
        assertThat(first.host()).isEqualTo(second.host());
        assertThat(first.port()).isEqualTo(second.port());
        var firstFactory = connectionFactory(first);
        var secondFactory = connectionFactory(second);
        try {
            var firstTemplate = new StringRedisTemplate(firstFactory);
            var secondTemplate = new StringRedisTemplate(secondFactory);
            firstTemplate.opsForValue().set("isolation-probe", "first");
            assertThat(secondTemplate.opsForValue().get("isolation-probe")).isNull();
            secondTemplate.opsForValue().set("isolation-probe", "second");
            assertThat(firstTemplate.opsForValue().get("isolation-probe")).isEqualTo("first");
            firstFactory.stop();
            assertThat(secondTemplate.opsForValue().get("isolation-probe")).isEqualTo("second");
        } finally {
            firstFactory.destroy();
            secondFactory.destroy();
        }
    }

    private LettuceConnectionFactory connectionFactory(SharedTestContainers.RedisDatabase database) {
        var factory = new LettuceConnectionFactory(database.host(), database.port());
        factory.setDatabase(database.database());
        factory.afterPropertiesSet();
        factory.start();
        return factory;
    }
}
