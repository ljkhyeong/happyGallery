package com.personal.happygallery.application.migration;

import com.personal.happygallery.application.crypto.SpringSecurityFieldEncryptor;
import com.personal.happygallery.bootstrap.migration.V46__ProtectPlaintextPersonalData;
import com.personal.happygallery.domain.crypto.BlindIndexer;
import com.personal.happygallery.domain.crypto.FieldEncryptor;
import java.time.LocalDateTime;
import java.util.HexFormat;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.mysql.MySQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class ReviewEvidenceMigrationTest {

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

    @Test
    @DisplayName("단계별 후기 증거 migration은 기존 신고를 이관하고 삭제 tombstone을 비식별화한다")
    void migrateReviewEvidence_backfillsLegacyDataAndRedactsDeletedTombstone() {
        String parameterSeparator = MYSQL.getJdbcUrl().contains("?") ? "&" : "?";
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                MYSQL.getJdbcUrl()
                        + parameterSeparator
                        + "sessionVariables=FOREIGN_KEY_CHECKS=0",
                MYSQL.getUsername(),
                MYSQL.getPassword());
        flyway(dataSource, "123").clean();
        flyway(dataSource, "123").migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 1, 10, 0);
        LocalDateTime decidedAt = createdAt.plusDays(1);

        jdbc.update("""
                INSERT INTO reviews (
                    id, user_id, order_item_id, product_id, rating, content,
                    status, created_at, updated_at, version
                ) VALUES (100, 100, 100, 100, 4, '현재 후기 본문',
                          'PUBLISHED', ?, ?, 0)
                """, createdAt, createdAt);
        jdbc.update("""
                INSERT INTO review_reports (
                    id, review_id, reporter_user_id, reason, detail,
                    snapshot_rating, snapshot_content, snapshot_status, snapshot_edited_at,
                    status, decision_note, decided_by_admin_id, decided_at, created_at
                ) VALUES (
                    201, 100, 101, 'OTHER', '미결 신고',
                    2, '미결 신고 당시 본문', 'PUBLISHED', NULL,
                    'PENDING', NULL, NULL, NULL, ?
                )
                """, createdAt);
        jdbc.update("""
                INSERT INTO reviews (
                    id, user_id, order_item_id, product_id, rating, content,
                    status, hidden_reason, hidden_at, hidden_by_admin_id,
                    deleted_at, recreation_blocked, created_at, updated_at, version
                ) VALUES (
                    101, 100, 101, 101, NULL, NULL,
                    'HIDDEN', '기존 숨김 사유', ?, 1,
                    ?, TRUE, ?, ?, 0
                )
                """, createdAt, createdAt.plusDays(2), createdAt, createdAt.plusDays(2));
        jdbc.update("""
                INSERT INTO review_reports (
                    id, review_id, reporter_user_id, reason, detail,
                    snapshot_rating, snapshot_content, snapshot_status, snapshot_edited_at,
                    status, decision_note, decided_by_admin_id, decided_at, created_at
                ) VALUES (
                    202, 100, 102, 'SPAM', '종결 신고',
                    1, '종결 신고 당시 본문', 'PUBLISHED', ?,
                    'REJECTED', '정책 위반 아님', 1, ?, ?
                )
                """, createdAt.minusHours(1), decidedAt, createdAt);
        jdbc.update("""
                INSERT INTO review_moderation_actions (
                    id, review_id, action, previous_status, new_status,
                    reason, admin_user_id, created_at
                ) VALUES (
                    301, 100, 'HIDE', 'PUBLISHED', 'HIDDEN',
                    '기존 운영 조치', 1, ?
                )
                """, createdAt);

        flyway(dataSource, "136").migrate();

        assertSoftly(softly -> {
            softly.assertThat(jdbc.queryForObject(
                            "SELECT content_revision FROM reviews WHERE id = 100", Long.class))
                    .isEqualTo(1L);
            softly.assertThat(jdbc.queryForObject("""
                            SELECT evidence_snapshot_id
                            FROM review_reports
                            WHERE id = 201
                            """, Long.class))
                    .isEqualTo(201L);
            softly.assertThat(jdbc.queryForObject("""
                            SELECT evidence_snapshot_id
                            FROM review_moderation_actions
                            WHERE id = 301
                            """, Long.class))
                    .isNull();
            softly.assertThat(jdbc.queryForMap("""
                            SELECT content_revision, rating, content, provenance,
                                   images_complete, retention_until
                            FROM review_evidence_snapshots
                            WHERE id = 201
                            """))
                    .containsEntry("content_revision", 1L)
                    .containsEntry("rating", 2)
                    .containsEntry("content", "미결 신고 당시 본문")
                    .containsEntry("provenance", "LEGACY_REPORT")
                    .containsEntry("images_complete", false)
                    .containsEntry("retention_until", null);
            softly.assertThat(jdbc.queryForObject("""
                            SELECT retention_until
                            FROM review_evidence_snapshots
                            WHERE id = 202
                            """, LocalDateTime.class))
                    .isEqualTo(decidedAt.plusYears(3));
            softly.assertThat(columnCount(jdbc, "review_reports", "snapshot_content")).isZero();
            softly.assertThat(jdbc.queryForMap("""
                            SELECT recreation_blocked, hidden_reason, hidden_at, hidden_by_admin_id
                            FROM reviews
                            WHERE id = 101
                            """))
                    .containsEntry("recreation_blocked", true)
                    .containsEntry("hidden_reason", null)
                    .containsEntry("hidden_at", null)
                    .containsEntry("hidden_by_admin_id", null);
        });
    }

    private Flyway flyway(DriverManagerDataSource dataSource, String target) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .javaMigrations(new V46__ProtectPlaintextPersonalData(fieldEncryptor, blindIndexer))
                .target(target)
                .cleanDisabled(false)
                .load();
    }

    private long columnCount(JdbcTemplate jdbc, String table, String column) {
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """, Long.class, table, column);
        return count == null ? 0L : count;
    }
}
