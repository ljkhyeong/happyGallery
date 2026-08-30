package com.personal.happygallery.application.migration;

import com.personal.happygallery.application.crypto.SpringSecurityFieldEncryptor;
import com.personal.happygallery.bootstrap.migration.V46__ProtectPlaintextPersonalData;
import com.personal.happygallery.domain.crypto.BlindIndexer;
import com.personal.happygallery.domain.crypto.FieldEncryptor;
import java.util.HexFormat;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.testcontainers.mysql.MySQLContainer;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

class PersonalDataMigrationTest {

    private static final String ENCRYPT_KEY =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String HMAC_KEY =
            "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789";

    private static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0");

    private final FieldEncryptor fieldEncryptor = new SpringSecurityFieldEncryptor(
            HexFormat.of().parseHex(ENCRYPT_KEY));
    private final BlindIndexer blindIndexer = new BlindIndexer(HexFormat.of().parseHex(HMAC_KEY));

    @BeforeAll
    static void startContainer() {
        MYSQL.start();
    }

    @AfterAll
    static void stopContainer() {
        MYSQL.stop();
    }

    @DisplayName("기존 평문 개인정보를 백필한 뒤 원본 컬럼을 제거한다")
    @Test
    void protectsLegacyPersonalDataAndDropsPlaintextColumns() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target("45")
                .load()
                .migrate();

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        long userId = insertLegacyData(jdbc);

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .javaMigrations(new V46__ProtectPlaintextPersonalData(fieldEncryptor, blindIndexer))
                .target("46")
                .load()
                .migrate();

        String emailEnc = jdbc.queryForObject(
                "SELECT email_enc FROM users WHERE id = ?", String.class, userId);
        String nameEnc = jdbc.queryForObject(
                "SELECT name_enc FROM users WHERE id = ?", String.class, userId);
        String phoneEnc = jdbc.queryForObject(
                "SELECT phone_enc FROM users WHERE id = ?", String.class, userId);
        String guestNameEnc = jdbc.queryForObject(
                "SELECT name_enc FROM guests LIMIT 1", String.class);
        String guestPhoneEnc = jdbc.queryForObject(
                "SELECT phone_enc FROM guests LIMIT 1", String.class);
        String payloadEnc = jdbc.queryForObject(
                "SELECT payload_enc FROM payment_attempt LIMIT 1", String.class);
        String providerIdHmac = jdbc.queryForObject(
                "SELECT provider_id_hmac FROM user_social_accounts LIMIT 1", String.class);

        assertSoftly(softly -> {
            softly.assertThat(fieldEncryptor.decrypt(emailEnc)).isEqualTo("member@example.com");
            softly.assertThat(fieldEncryptor.decrypt(nameEnc)).isEqualTo("회원 이름");
            softly.assertThat(fieldEncryptor.decrypt(phoneEnc)).isEqualTo("01012345678");
            softly.assertThat(fieldEncryptor.decrypt(guestNameEnc)).isEqualTo("게스트 이름");
            softly.assertThat(fieldEncryptor.decrypt(guestPhoneEnc)).isEqualTo("01087654321");
            softly.assertThat(fieldEncryptor.decrypt(payloadEnc))
                    .contains("010-8765-4321", "123456", "게스트 이름");
            softly.assertThat(providerIdHmac).isEqualTo(blindIndexer.index("google-provider-id"));
            softly.assertThat(JdbcTestUtils.countRowsInTable(jdbc, "phone_verifications"))
                    .isZero();
            softly.assertThat(hasColumn(jdbc, "users", "email")).isFalse();
            softly.assertThat(hasColumn(jdbc, "users", "name")).isFalse();
            softly.assertThat(hasColumn(jdbc, "users", "phone")).isFalse();
            softly.assertThat(hasColumn(jdbc, "guests", "name")).isFalse();
            softly.assertThat(hasColumn(jdbc, "payment_attempt", "payload_json")).isFalse();
            softly.assertThat(hasColumn(jdbc, "phone_verifications", "code")).isFalse();
            softly.assertThat(hasColumn(jdbc, "user_social_accounts", "provider_id")).isFalse();
            softly.assertThat(hasColumn(jdbc, "fulfillments", "address")).isFalse();
        });
    }

    private long insertLegacyData(JdbcTemplate jdbc) {
        jdbc.update("""
                INSERT INTO users (email, password_hash, name, phone, phone_verified)
                VALUES ('Member@Example.com', 'hash', '회원 이름', '010-1234-5678', FALSE)
                """);
        Long userId = jdbc.queryForObject("SELECT id FROM users LIMIT 1", Long.class);
        jdbc.update("""
                        INSERT INTO guests (name, phone_enc, phone_hmac, phone_verified)
                        VALUES ('게스트 이름', ?, ?, TRUE)
                        """,
                fieldEncryptor.encrypt("010-8765-4321"),
                blindIndexer.index("010-8765-4321"));
        jdbc.update("""
                INSERT INTO phone_verifications (phone, code, verified, expires_at)
                VALUES ('010-8765-4321', '123456', FALSE, DATE_ADD(NOW(6), INTERVAL 5 MINUTE))
                """);
        jdbc.update("""
                INSERT INTO payment_attempt (order_id_external, context, amount, status, payload_json)
                VALUES ('migration-payment', 'BOOKING', 5000, 'PENDING',
                        '{"type":"BOOKING","phone":"010-8765-4321","verificationCode":"123456","name":"게스트 이름"}')
                """);
        jdbc.update("""
                INSERT INTO user_social_accounts (user_id, provider, provider_id)
                VALUES (?, 'GOOGLE', 'google-provider-id')
                """, userId);
        return userId;
    }

    private boolean hasColumn(JdbcTemplate jdbc, String table, String column) {
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """, Long.class, table, column);
        return count > 0;
    }
}
