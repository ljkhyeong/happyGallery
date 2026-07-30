package com.personal.happygallery.application.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.personal.happygallery.application.crypto.SpringSecurityFieldEncryptor;
import com.personal.happygallery.bootstrap.migration.V46__ProtectPlaintextPersonalData;
import com.personal.happygallery.domain.crypto.BlindIndexer;
import com.personal.happygallery.domain.crypto.FieldEncryptor;
import java.util.HexFormat;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.mysql.MySQLContainer;

class DomainInvariantMigrationTest {

    private static final String ENCRYPT_KEY =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String HMAC_KEY =
            "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789";
    private static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0");

    private final FieldEncryptor fieldEncryptor = new SpringSecurityFieldEncryptor(
            HexFormat.of().parseHex(ENCRYPT_KEY));
    private final BlindIndexer blindIndexer = new BlindIndexer(HexFormat.of().parseHex(HMAC_KEY));
    private DriverManagerDataSource dataSource;

    @BeforeAll
    static void startContainer() {
        MYSQL.start();
    }

    @AfterAll
    static void stopContainer() {
        MYSQL.stop();
    }

    @BeforeEach
    void cleanDatabase() {
        dataSource = new DriverManagerDataSource(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        flyway("106").clean();
    }

    @DisplayName("V105는 소유자 없는 기존 8회권이 있으면 atomic ALTER 전체를 실패시킨다")
    @Test
    void migrateV105_nullPassOwner_failsWithoutPartialConstraint() {
        flyway("104").migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("""
                INSERT INTO pass_purchases (
                    user_id, purchased_at, expires_at, plan_code,
                    total_credits, remaining_credits, total_price, version
                ) VALUES (
                    NULL, NOW(6), DATE_ADD(NOW(6), INTERVAL 90 DAY), 'REGULAR_CRAFT_8',
                    8, 8, 240000, 0
                )
                """);

        Throwable failure = catchThrowable(() -> flyway("105").migrate());

        assertThat(failure).isInstanceOf(FlywayException.class);
        assertThat(NestedExceptionUtils.getMostSpecificCause(failure).getMessage())
                .contains("user_id");
        assertThat(columnNullable(jdbc, "pass_purchases", "user_id")).isEqualTo("YES");
        assertThat(constraintCount(jdbc, "pass_purchases", "fk_pass_user")).isEqualTo(1L);
        assertThat(constraintCount(jdbc, "pass_purchases", "fk_pass_user_v105")).isZero();
    }

    @DisplayName("V105와 V106은 8회권 소유자와 환불 양수 금액을 DB에서도 강제한다")
    @Test
    void migrateV106_freshSchema_enforcesPassOwnerAndPositiveRefund() {
        flyway("106").migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        assertThat(columnNullable(jdbc, "pass_purchases", "user_id")).isEqualTo("NO");
        assertThat(constraintCount(jdbc, "pass_purchases", "fk_pass_user")).isZero();
        assertThat(constraintCount(jdbc, "pass_purchases", "fk_pass_user_v105")).isEqualTo(1L);
        assertThat(constraintCount(jdbc, "refunds", "chk_refunds_amount_positive"))
                .isEqualTo(1L);

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO pass_purchases (
                    user_id, purchased_at, expires_at, plan_code,
                    total_credits, remaining_credits, total_price, version
                ) VALUES (
                    NULL, NOW(6), DATE_ADD(NOW(6), INTERVAL 90 DAY), 'REGULAR_CRAFT_8',
                    8, 8, 240000, 0
                )
                """))
                .isInstanceOf(DataAccessException.class);

        jdbc.update("""
                INSERT INTO payment_attempt (
                    order_id_external, context, amount, status, payload_enc
                ) VALUES ('v105-refund-source', 'ORDER', 1000, 'PENDING', NULL)
                """);
        Long paymentAttemptId = jdbc.queryForObject(
                "SELECT id FROM payment_attempt WHERE order_id_external = 'v105-refund-source'",
                Long.class);

        assertThatThrownBy(() -> jdbc.update("""
                        INSERT INTO refunds (
                            payment_attempt_id, amount, status, idempotency_key
                        ) VALUES (?, 0, 'REQUESTED', 'v105-zero-refund')
                        """, paymentAttemptId))
                .isInstanceOf(DataAccessException.class);
    }

    @DisplayName("V106은 0원 기존 환불이 있으면 CHECK 제약을 적용하지 않고 실패한다")
    @Test
    void migrateV106_zeroRefund_failsWithoutPartialConstraint() {
        flyway("105").migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("""
                INSERT INTO payment_attempt (
                    order_id_external, context, amount, status, payload_enc
                ) VALUES ('v106-dirty-refund-source', 'ORDER', 1000, 'PENDING', NULL)
                """);
        Long paymentAttemptId = jdbc.queryForObject(
                "SELECT id FROM payment_attempt WHERE order_id_external = 'v106-dirty-refund-source'",
                Long.class);
        jdbc.update("""
                INSERT INTO refunds (
                    payment_attempt_id, amount, status, idempotency_key
                ) VALUES (?, 0, 'REQUESTED', 'v106-dirty-zero-refund')
                """, paymentAttemptId);

        Throwable failure = catchThrowable(() -> flyway("106").migrate());

        assertThat(failure).isInstanceOf(FlywayException.class);
        assertThat(NestedExceptionUtils.getMostSpecificCause(failure).getMessage())
                .contains("chk_refunds_amount_positive");
        assertThat(constraintCount(jdbc, "refunds", "chk_refunds_amount_positive")).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM refunds WHERE amount <= 0", Long.class)).isEqualTo(1L);
    }

    private Flyway flyway(String target) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .javaMigrations(new V46__ProtectPlaintextPersonalData(fieldEncryptor, blindIndexer))
                .target(target)
                .cleanDisabled(false)
                .load();
    }

    private long constraintCount(JdbcTemplate jdbc, String table, String constraint) {
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND constraint_name = ?
                """, Long.class, table, constraint);
        return count == null ? 0L : count;
    }

    private String columnNullable(JdbcTemplate jdbc, String table, String column) {
        return jdbc.queryForObject("""
                SELECT is_nullable
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """, String.class, table, column);
    }

}
