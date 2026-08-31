package com.personal.happygallery.application.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.personal.happygallery.application.crypto.SpringSecurityFieldEncryptor;
import com.personal.happygallery.bootstrap.migration.V46__ProtectPlaintextPersonalData;
import com.personal.happygallery.domain.crypto.BlindIndexer;
import com.personal.happygallery.domain.crypto.FieldEncryptor;
import java.util.HexFormat;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
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
        flyway("121").clean();
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

    @DisplayName("V109부터 V115는 공유 복구 토큰과 고객 이력 커서를 비유일 복합 인덱스로 지원한다")
    @Test
    void migrateV115_freshSchema_createsRecoveryAndHistoryCursorIndexes() {
        flyway("115").migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        assertThat(indexColumns(jdbc, "orders", "idx_orders_access_token_created"))
                .containsExactly("access_token", "created_at", "id");
        assertThat(indexColumns(jdbc, "bookings", "idx_bookings_access_token_created"))
                .containsExactly("access_token", "created_at", "id");
        assertThat(indexNonUnique(jdbc, "orders", "idx_orders_access_token_created"))
                .isEqualTo(1);
        assertThat(indexNonUnique(jdbc, "bookings", "idx_bookings_access_token_created"))
                .isEqualTo(1);
        assertThat(indexColumns(jdbc, "orders", "idx_orders_user_created_id"))
                .containsExactly("user_id", "created_at", "id");
        assertThat(indexColumns(jdbc, "bookings", "idx_bookings_user_created_id"))
                .containsExactly("user_id", "created_at", "id");
        assertThat(indexColumns(jdbc, "pass_purchases", "idx_pass_purchases_user_purchased_id"))
                .containsExactly("user_id", "purchased_at", "id");
        assertThat(indexColumns(jdbc, "inquiry", "idx_inquiry_user_created_id"))
                .containsExactly("user_id", "created_at", "id");
        assertThat(indexColumns(jdbc, "product_qna", "idx_product_qna_product_created_id"))
                .containsExactly("product_id", "created_at", "id");
        assertThat(indexColumns(
                jdbc, "product_qna", "idx_product_qna_product_user_created_id"))
                .containsExactly("product_id", "user_id", "created_at", "id");
        assertThat(indexColumns(jdbc, "orders", "uq_orders_access_token")).isEmpty();
        assertThat(indexColumns(jdbc, "bookings", "uq_bookings_access_token")).isEmpty();
    }

    @DisplayName("V120은 기존 환불을 고객 반환액으로 이관하고 PG 0원은 주문 혜택 환불에만 허용한다")
    @Test
    void migrateV120_backfillsCustomerRefundAndScopesZeroPgRefund() {
        flyway("119").migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("""
                INSERT INTO payment_attempt (
                    order_id_external, context, amount, status, payload_enc
                ) VALUES ('v120-refund-source', 'ORDER', 1000, 'PENDING', NULL)
                """);
        Long paymentAttemptId = jdbc.queryForObject(
                "SELECT id FROM payment_attempt WHERE order_id_external = 'v120-refund-source'",
                Long.class);
        jdbc.update("""
                INSERT INTO refunds (
                    payment_attempt_id, amount, status, idempotency_key
                ) VALUES (?, 1000, 'REQUESTED', 'v120-legacy-refund')
                """, paymentAttemptId);

        flyway("120").migrate();

        assertThat(constraintCount(jdbc, "refunds", "chk_refunds_amount_positive")).isZero();
        assertThat(constraintCount(jdbc, "refunds", "chk_refunds_mixed_benefit_amounts"))
                .isEqualTo(1L);
        assertThat(jdbc.queryForObject("""
                SELECT customer_refund_amount
                FROM refunds
                WHERE idempotency_key = 'v120-legacy-refund'
                """, Long.class)).isEqualTo(1_000L);
        jdbc.update("""
                INSERT INTO payment_attempt (
                    order_id_external, context, amount, status, payload_enc
                ) VALUES ('v120-zero-refund-source', 'ORDER', 1000, 'PENDING', NULL)
                """);
        Long zeroRefundAttemptId = jdbc.queryForObject(
                "SELECT id FROM payment_attempt WHERE order_id_external = 'v120-zero-refund-source'",
                Long.class);
        assertConstraintViolation("chk_refunds_mixed_benefit_amounts", () -> jdbc.update("""
                INSERT INTO refunds (
                    payment_attempt_id, amount, customer_refund_amount,
                    reward_restore_amount, reward_revoke_amount, restore_coupon,
                    status, idempotency_key
                ) VALUES (?, 0, 0, 0, 0, FALSE, 'REQUESTED', 'v120-invalid-local-refund')
                """, zeroRefundAttemptId));
    }

    @DisplayName("V121은 적립금 예약 상태와 주문 혜택 스냅샷 제약을 DB에서 강제한다")
    @Test
    void migrateV121_freshSchema_enforcesRewardAndOrderBenefitConstraints() {
        flyway("121").migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        BenefitConstraintFixture fixture = createBenefitConstraintFixture(jdbc);

        assertThat(constraintCount(
                jdbc, "reward_reservations", "chk_reward_reservation_state")).isEqualTo(1L);
        assertThat(constraintCount(
                jdbc, "order_items", "chk_order_items_price_arithmetic")).isEqualTo(1L);
        assertThat(constraintCount(
                jdbc, "orders", "fk_orders_issued_coupon")).isEqualTo(1L);

        assertConstraintViolation("chk_reward_reservation_state", () -> jdbc.update("""
                INSERT INTO reward_reservations (
                    payment_attempt_id, user_id, order_id, amount,
                    restored_amount, status, resolved_at
                ) VALUES (?, ?, NULL, 100, 0, 'UNKNOWN', NULL)
                """, fixture.paymentAttemptId(), fixture.userId()));
        assertConstraintViolation("chk_reward_reservation_state", () -> jdbc.update("""
                INSERT INTO reward_reservations (
                    payment_attempt_id, user_id, order_id, amount,
                    restored_amount, status, resolved_at
                ) VALUES (?, ?, ?, 100, 0, 'RESERVED', NULL)
                """, fixture.paymentAttemptId(), fixture.userId(), fixture.orderId()));
        assertConstraintViolation("chk_reward_reservation_state", () -> jdbc.update("""
                INSERT INTO reward_reservations (
                    payment_attempt_id, user_id, order_id, amount,
                    restored_amount, status, resolved_at
                ) VALUES (?, ?, NULL, 100, 0, 'RESERVED', NOW(6))
                """, fixture.paymentAttemptId(), fixture.userId()));
        assertConstraintViolation("chk_reward_reservation_state", () -> jdbc.update("""
                INSERT INTO reward_reservations (
                    payment_attempt_id, user_id, order_id, amount,
                    restored_amount, status, resolved_at
                ) VALUES (?, ?, NULL, 100, 1, 'RELEASED', NOW(6))
                """, fixture.paymentAttemptId(), fixture.userId()));

        jdbc.update("""
                INSERT INTO reward_reservations (
                    payment_attempt_id, user_id, order_id, amount,
                    restored_amount, status, resolved_at
                ) VALUES (?, ?, NULL, 100, 0, 'RESERVED', NULL)
                """, fixture.paymentAttemptId(), fixture.userId());
        jdbc.update("""
                UPDATE reward_reservations
                SET order_id = ?, restored_amount = 50, status = 'USED', resolved_at = NOW(6)
                WHERE payment_attempt_id = ?
                """, fixture.orderId(), fixture.paymentAttemptId());

        assertConstraintViolation(
                "chk_order_items_price_arithmetic",
                () -> insertOrderItem(jdbc, fixture, 0, 1_000L, 1_000L));
        assertConstraintViolation(
                "chk_order_items_price_arithmetic",
                () -> insertOrderItem(jdbc, fixture, 1, 0L, 1_000L));
        assertConstraintViolation(
                "chk_order_items_price_arithmetic",
                () -> insertOrderItem(jdbc, fixture, 2, 1_000L, 1_500L));
        insertOrderItem(jdbc, fixture, 2, 1_000L, 2_000L);

        assertConstraintViolation("fk_orders_issued_coupon", () -> jdbc.update("""
                UPDATE orders
                SET total_amount = 900,
                    coupon_discount_amount = 100,
                    pg_paid_amount = 900,
                    reward_earn_base = 900,
                    issued_coupon_id = 999999999
                WHERE id = ?
                """, fixture.orderId()));
        assertThat(jdbc.queryForObject(
                "SELECT issued_coupon_id FROM orders WHERE id = ?",
                Long.class,
                fixture.orderId())).isNull();
    }

    @DisplayName("V121은 잘못된 적립금 예약 상태가 있으면 어떤 영구 제약도 부분 적용하지 않는다")
    @Test
    void migrateV121_dirtyRewardReservation_failsBeforePermanentConstraints() {
        flyway("120").migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        BenefitConstraintFixture fixture = createBenefitConstraintFixture(jdbc);
        jdbc.update("""
                INSERT INTO reward_reservations (
                    payment_attempt_id, user_id, order_id, amount,
                    restored_amount, status, resolved_at
                ) VALUES (?, ?, ?, 100, 0, 'RESERVED', NULL)
                """, fixture.paymentAttemptId(), fixture.userId(), fixture.orderId());

        Throwable failure = catchThrowable(() -> flyway("121").migrate());

        assertThat(failure).isInstanceOf(FlywayException.class);
        assertThat(NestedExceptionUtils.getMostSpecificCause(failure).getMessage())
                .contains("chk_v121_reward_reservation_state_preflight");
        assertV121PermanentConstraintsAbsent(jdbc);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM reward_reservations
                WHERE status = 'RESERVED' AND order_id IS NOT NULL
                """, Long.class)).isEqualTo(1L);
    }

    @DisplayName("V121은 잘못된 주문 품목이 있으면 어떤 영구 제약도 부분 적용하지 않는다")
    @Test
    void migrateV121_dirtyOrderItem_failsBeforePermanentConstraints() {
        flyway("120").migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        BenefitConstraintFixture fixture = createBenefitConstraintFixture(jdbc);
        insertOrderItem(jdbc, fixture, 0, 1_000L, 1_000L);

        Throwable failure = catchThrowable(() -> flyway("121").migrate());

        assertThat(failure).isInstanceOf(FlywayException.class);
        assertThat(NestedExceptionUtils.getMostSpecificCause(failure).getMessage())
                .contains("chk_v121_order_item_price_arithmetic_preflight");
        assertV121PermanentConstraintsAbsent(jdbc);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM order_items WHERE qty = 0", Long.class)).isEqualTo(1L);
    }

    @DisplayName("V121은 고아 발급 쿠폰 참조가 있으면 어떤 영구 제약도 부분 적용하지 않는다")
    @Test
    void migrateV121_orphanIssuedCoupon_failsBeforePermanentConstraints() {
        flyway("120").migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        BenefitConstraintFixture fixture = createBenefitConstraintFixture(jdbc);
        long orphanIssuedCouponId = 999_999_999L;
        jdbc.update("""
                UPDATE orders
                SET total_amount = 900,
                    coupon_discount_amount = 100,
                    pg_paid_amount = 900,
                    reward_earn_base = 900,
                    issued_coupon_id = ?
                WHERE id = ?
                """, orphanIssuedCouponId, fixture.orderId());

        Throwable failure = catchThrowable(() -> flyway("121").migrate());

        assertThat(failure).isInstanceOf(FlywayException.class);
        assertThat(NestedExceptionUtils.getMostSpecificCause(failure).getMessage())
                .contains("issued_coupon_id");
        assertV121PermanentConstraintsAbsent(jdbc);
        assertThat(jdbc.queryForObject(
                "SELECT issued_coupon_id FROM orders WHERE id = ?",
                Long.class,
                fixture.orderId())).isEqualTo(orphanIssuedCouponId);
    }

    private BenefitConstraintFixture createBenefitConstraintFixture(JdbcTemplate jdbc) {
        long userId = 9_121_001L;
        long paymentAttemptId = 9_121_002L;
        long productId = 9_121_003L;
        long orderId = 9_121_004L;
        jdbc.update("""
                INSERT INTO users (id, name_enc, name_hmac)
                VALUES (?, 'v121-user-name-enc', REPEAT('a', 64))
                """, userId);
        jdbc.update("""
                INSERT INTO payment_attempt (
                    id, order_id_external, context, amount, status, payload_enc
                ) VALUES (?, 'v121-payment-attempt', 'ORDER', 1000, 'PENDING', NULL)
                """, paymentAttemptId);
        jdbc.update("""
                INSERT INTO products (id, name, type, price, status)
                VALUES (?, 'V121 제약 검증 상품', 'READY_STOCK', 1000, 'ACTIVE')
                """, productId);
        jdbc.update("""
                INSERT INTO orders (
                    id, user_id, guest_id, status,
                    total_amount, product_amount, shipping_fee,
                    coupon_discount_amount, reward_used_amount, pg_paid_amount,
                    reward_earn_base, issued_coupon_id,
                    paid_at, approval_deadline_at
                ) VALUES (
                    ?, ?, NULL, 'PAID_APPROVAL_PENDING',
                    1000, 1000, 0,
                    0, 0, 1000,
                    1000, NULL,
                    NOW(6), DATE_ADD(NOW(6), INTERVAL 1 DAY)
                )
                """, orderId, userId);
        return new BenefitConstraintFixture(userId, paymentAttemptId, productId, orderId);
    }

    @Test
    @DisplayName("V164는 옵션 키 순으로 조합을 정렬하고 중복된 과거 조합의 번호와 참조를 보존한다")
    void migrateV164_preservesVariantIdsAndReferences() {
        flyway("163").migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("""
                INSERT INTO products (id, name, type, price, status, specification, production_lead_days)
                VALUES (164, '조합 전환 상품', 'MADE_TO_ORDER', 10000, 'ACTIVE', '가죽 키링', 3)
                """);
        jdbc.update("""
                INSERT INTO product_variants (id, product_id, combination_key, quantity, active)
                VALUES (1641, 164, 'a=two|a-z=one', 2, FALSE),
                       (1642, 164, 'a-z=one|a=two', 4, TRUE),
                       (1643, 164, 'DEFAULT', 1, FALSE),
                       (1644, 164, 'a-z=-|a=two', 7, TRUE)
                """);
        jdbc.update("""
                INSERT INTO users (id, name_enc, name_hmac)
                VALUES (164, 'v164-user', REPEAT('b', 64))
                """);
        jdbc.update("""
                INSERT INTO cart_items (id, user_id, product_id, product_variant_id, qty, line_key)
                VALUES (1641, 164, 164, 1641, 1, REPEAT('1', 64)),
                       (1642, 164, 164, 1642, 2, REPEAT('2', 64))
                """);

        flyway("164").migrate();

        assertThat(jdbc.queryForList("SELECT combination_key FROM product_variants ORDER BY id", String.class))
                .containsExactly("legacy-variant:1641", "a=two|a-z=one", "DEFAULT", "a=two|a-z=-");
        assertThat(jdbc.queryForList("SELECT quantity FROM product_variants ORDER BY id", Integer.class))
                .containsExactly(2, 4, 1, 7);
        assertThat(jdbc.queryForList("SELECT id FROM product_variants WHERE active = TRUE ORDER BY id", Long.class))
                .containsExactly(1642L, 1644L);
        assertThat(jdbc.queryForList("SELECT product_variant_id FROM cart_items ORDER BY id", Long.class))
                .containsExactly(1641L, 1642L);
        assertThat(jdbc.queryForList("SELECT qty FROM cart_items ORDER BY id", Integer.class))
                .containsExactly(1, 2);
    }

    private int insertOrderItem(JdbcTemplate jdbc,
                                BenefitConstraintFixture fixture,
                                int qty,
                                long unitPrice,
                                long grossAmount) {
        return jdbc.update("""
                INSERT INTO order_items (
                    order_id, product_id, product_name, product_type,
                    qty, unit_price, gross_amount,
                    coupon_discount_amount, reward_used_amount, net_paid_amount
                ) VALUES (?, ?, 'V121 제약 검증 상품', 'READY_STOCK', ?, ?, ?, 0, 0, ?)
                """, fixture.orderId(), fixture.productId(),
                qty, unitPrice, grossAmount, grossAmount);
    }

    private void assertConstraintViolation(String constraintName, ThrowingCallable sql) {
        Throwable failure = catchThrowable(sql);
        assertThat(failure).isInstanceOf(DataAccessException.class);
        assertThat(NestedExceptionUtils.getMostSpecificCause(failure).getMessage())
                .contains(constraintName);
    }

    private void assertV121PermanentConstraintsAbsent(JdbcTemplate jdbc) {
        assertThat(constraintCount(
                jdbc, "reward_reservations", "chk_reward_reservation_state")).isZero();
        assertThat(constraintCount(
                jdbc, "order_items", "chk_order_items_price_arithmetic")).isZero();
        assertThat(constraintCount(
                jdbc, "orders", "fk_orders_issued_coupon")).isZero();
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

    @Test
    @DisplayName("스마트스토어 전환은 기존 연결과 요청을 보존하고 같은 조합의 과거 연결만 중복 허용한다")
    void migrateSmartStoreMappings_preservesExistingRowsAndCurrentMappingUniqueness() {
        flyway("164").migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("""
                INSERT INTO products (id, name, type, price, status, specification, production_lead_days)
                VALUES (50001, '연결 전환 상품', 'MADE_TO_ORDER', 10000, 'ACTIVE', '가죽 제품', 7)
                """);
        jdbc.update("""
                INSERT INTO product_variants (id, product_id, combination_key, quantity)
                VALUES (50002, 50001, 'DEFAULT', 5)
                """);
        jdbc.update("""
                INSERT INTO smartstore_stock_mappings (id, product_id, product_variant_id, origin_product_no, option_id)
                VALUES (50003, 50001, 50002, 123, 100)
                """);
        jdbc.update("""
                INSERT INTO smartstore_stock_syncs (product_id, request_version, status, next_attempt_at)
                VALUES (50001, 7, 'PENDING', '2026-08-31 12:00:00')
                """);

        flyway("166").migrate();
        assertThat(jdbc.queryForMap("""
                SELECT generation, request_version, status FROM smartstore_stock_syncs WHERE product_id = 50001
                """)).containsEntry("generation", "legacy").containsEntry("request_version", 7L)
                .containsEntry("status", "PENDING");
        assertThat(jdbc.queryForObject("SELECT internal_target_key FROM smartstore_stock_mappings WHERE id = 50003",
                Long.class)).isEqualTo(50002L);
        jdbc.update("""
                INSERT INTO smartstore_stock_mappings (product_id, product_variant_id, origin_product_no, option_id, retired)
                VALUES (50001, 50002, 123, 101, TRUE), (50001, 50002, 123, 102, TRUE)
                """);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM smartstore_stock_mappings WHERE product_id = 50001",
                Long.class)).isEqualTo(3L);
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO smartstore_stock_mappings (product_id, product_variant_id, origin_product_no, option_id)
                VALUES (50001, 50002, 123, 103)
                """)).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO smartstore_stock_mappings (product_id, product_variant_id, origin_product_no, option_id, retired)
                VALUES (50001, 50002, 123, 100, TRUE)
                """)).isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("V167·V168은 기존 반품 검수 결과와 차감 수량을 보존하고 누적 반품 수량은 재수집 전까지 미설정으로 둔다")
    void migrateV167AndV168_preservesCompletedReturnReviews() {
        flyway("166").migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("""
                INSERT INTO smartstore_product_orders (
                    product_order_id, order_id, origin_product_no, product_name,
                    product_order_status, initial_quantity, remain_quantity,
                    inventory_applied_quantity, attention_reason, last_changed_type, last_changed_at
                ) VALUES
                    ('no-restock', 'o-1', 123, '검수 상품', 'RETURNED', 3, 1, 3, NULL,
                     'CLAIM_COMPLETED', '2026-08-31 12:00:00'),
                    ('restocked', 'o-2', 123, '검수 상품', 'RETURNED', 3, 1, 1, NULL,
                     'CLAIM_COMPLETED', '2026-08-31 12:00:00'),
                    ('pending', 'o-3', 123, '검수 상품', 'RETURNED', 3, 1, 3, 'RETURN_REVIEW',
                     'CLAIM_COMPLETED', '2026-08-31 12:00:00'),
                    ('paid', 'o-4', 123, '검수 상품', 'PAYED', 3, 3, 3, NULL,
                     'PAYED', '2026-08-31 12:00:00')
                """);
        var previousOrders = jdbc.queryForList("""
                SELECT product_order_id, inventory_applied_quantity, attention_reason, last_changed_at, updated_at
                FROM smartstore_product_orders ORDER BY product_order_id
                """);

        flyway("167").migrate();
        flyway("168").migrate();

        assertThat(jdbc.queryForList("""
                SELECT return_reviewed_remain_quantity FROM smartstore_product_orders ORDER BY product_order_id
                """, Integer.class)).containsExactly(1, null, null, 1);
        assertThat(jdbc.queryForList("""
                SELECT completed_return_quantity FROM smartstore_product_orders ORDER BY product_order_id
                """, Integer.class)).containsOnlyNulls();
        assertThat(jdbc.queryForList("""
                SELECT reviewed_return_quantity FROM smartstore_product_orders ORDER BY product_order_id
                """, Integer.class)).containsExactly(0, 0, 0, 0);
        assertThat(jdbc.queryForList("""
                SELECT restored_return_quantity FROM smartstore_product_orders ORDER BY product_order_id
                """, Integer.class)).containsExactly(0, 0, 0, 0);
        assertThat(jdbc.queryForList("""
                SELECT product_order_id, inventory_applied_quantity, attention_reason, last_changed_at, updated_at
                FROM smartstore_product_orders ORDER BY product_order_id
                """)).isEqualTo(previousOrders);
    }

    private long constraintCount(JdbcTemplate jdbc, String table, String constraint) {
        return jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND constraint_name = ?
                """, Long.class, table, constraint);
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

    private List<String> indexColumns(JdbcTemplate jdbc, String table, String index) {
        return jdbc.queryForList("""
                SELECT column_name
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND index_name = ?
                ORDER BY seq_in_index
                """, String.class, table, index);
    }

    private int indexNonUnique(JdbcTemplate jdbc, String table, String index) {
        Integer nonUnique = jdbc.queryForObject("""
                SELECT MIN(non_unique)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND index_name = ?
                """, Integer.class, table, index);
        return nonUnique == null ? -1 : nonUnique;
    }

    private record BenefitConstraintFixture(
            long userId,
            long paymentAttemptId,
            long productId,
            long orderId
    ) {}

}
