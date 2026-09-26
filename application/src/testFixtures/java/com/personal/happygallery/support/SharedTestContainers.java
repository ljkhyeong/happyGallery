package com.personal.happygallery.support;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/** JVM 안에서 서버만 공유한다. DB와 Redis 키 공간은 호출자마다 분리하며 종료는 Ryuk이 맡는다. */
public final class SharedTestContainers {
    private SharedTestContainers() {}

    public static Database newDatabase() {
        MySQLContainer mysql = MysqlServer.CONTAINER;
        String schema = "hg_test_" + MysqlServer.DATABASE_SEQUENCE.incrementAndGet();
        try (var connection = DriverManager.getConnection(
                mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
             var statement = connection.createStatement()) {
            statement.executeUpdate("CREATE DATABASE " + schema);
        } catch (SQLException exception) {
            throw new IllegalStateException("격리된 테스트 DB를 만들지 못했습니다.", exception);
        }
        return new Database(mysql.getJdbcUrl().replace("/" + mysql.getDatabaseName(), "/" + schema),
                mysql.getUsername(), mysql.getPassword());
    }

    public static RedisDatabase newRedisDatabase() {
        int database = RedisServer.DATABASE_SEQUENCE.getAndIncrement();
        if (database >= RedisServer.DATABASE_COUNT) {
            throw new IllegalStateException("테스트 Redis DB 할당 한도를 초과했습니다.");
        }
        return new RedisDatabase(RedisServer.CONTAINER.getHost(),
                RedisServer.CONTAINER.getMappedPort(6379), database);
    }

    public record Database(String jdbcUrl, String username, String password) {}

    public record RedisDatabase(String host, int port, int database) {}

    private static final class MysqlServer {
        private static final AtomicInteger DATABASE_SEQUENCE = new AtomicInteger();
        private static final MySQLContainer CONTAINER = new MySQLContainer("mysql:8.0")
                .withUsername("root");

        static {
            CONTAINER.start();
        }
    }

    private static final class RedisServer {
        private static final int DATABASE_COUNT = 256;
        private static final AtomicInteger DATABASE_SEQUENCE = new AtomicInteger();
        private static final GenericContainer<?> CONTAINER =
                new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                        .withCommand("redis-server", "--notify-keyspace-events", "Egx",
                                "--databases", Integer.toString(DATABASE_COUNT))
                        .withExposedPorts(6379);

        static {
            CONTAINER.start();
        }
    }
}
