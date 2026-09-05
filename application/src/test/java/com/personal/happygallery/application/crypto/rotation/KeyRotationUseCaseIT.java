package com.personal.happygallery.application.crypto.rotation;

import com.personal.happygallery.adapter.out.persistence.booking.BookingRepository;
import com.personal.happygallery.adapter.out.persistence.booking.ClassRepository;
import com.personal.happygallery.adapter.out.persistence.booking.GuestRepository;
import com.personal.happygallery.adapter.out.persistence.booking.PhoneVerificationRepository;
import com.personal.happygallery.adapter.out.persistence.booking.SlotRepository;
import com.personal.happygallery.adapter.out.persistence.order.FulfillmentRepository;
import com.personal.happygallery.adapter.out.persistence.order.OrderRepository;
import com.personal.happygallery.adapter.out.persistence.payment.PaymentAttemptRepository;
import com.personal.happygallery.adapter.out.persistence.user.EmailVerificationRepository;
import com.personal.happygallery.adapter.out.persistence.user.SocialAccountRepository;
import com.personal.happygallery.application.admin.port.out.AdminUserPort;
import com.personal.happygallery.application.crypto.SpringSecurityFieldEncryptor;
import com.personal.happygallery.application.crypto.VersionedFieldEncryptor;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.domain.admin.AdminUser;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.booking.PhoneVerification;
import com.personal.happygallery.domain.booking.PhoneVerificationPurpose;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.crypto.BlindIndexKeyRing;
import com.personal.happygallery.domain.crypto.BlindIndexer;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentContext;
import com.personal.happygallery.domain.user.SocialAccount;
import com.personal.happygallery.domain.user.SocialProvider;
import com.personal.happygallery.domain.user.EmailVerification;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@UseCaseIT
@TestPropertySource(properties = {
        "app.field-encryption.active-key-id=v2",
        "app.field-encryption.encrypt-key=1111111111111111111111111111111111111111111111111111111111111111",
        "app.field-encryption.hmac-key=2222222222222222222222222222222222222222222222222222222222222222",
        "app.field-encryption.previous-encrypt-keys=v1=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "app.field-encryption.previous-hmac-keys=v1=abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789"
})
class KeyRotationUseCaseIT {

    private static final String OLD_AES_HEX =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String OLD_HMAC_HEX =
            "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789";

    private final SpringSecurityFieldEncryptor oldEncryptor = new SpringSecurityFieldEncryptor(
            HexFormat.of().parseHex(OLD_AES_HEX));
    private final BlindIndexer oldIndexer = new BlindIndexer(HexFormat.of().parseHex(OLD_HMAC_HEX));

    @Autowired KeyRotationUseCase keyRotationUseCase;
    @Autowired VersionedFieldEncryptor activeEncryptor;
    @Autowired BlindIndexKeyRing activeIndexKeyRing;
    @Autowired UserStorePort userStorePort;
    @Autowired GuestRepository guestRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired ClassRepository classRepository;
    @Autowired SlotRepository slotRepository;
    @Autowired PaymentAttemptRepository paymentAttemptRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired FulfillmentRepository fulfillmentRepository;
    @Autowired PhoneVerificationRepository phoneVerificationRepository;
    @Autowired EmailVerificationRepository emailVerificationRepository;
    @Autowired SocialAccountRepository socialAccountRepository;
    @Autowired AdminUserPort adminUserPort;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired TestCleanupSupport cleanupSupport;

    @AfterEach
    void tearDown() {
        cleanup();
    }

    @DisplayName("구키 개인정보를 전환하고 이메일 없는 회원과 원문 없는 소셜 계정을 안전하게 처리한다")
    @Test
    void rotate_reencryptsProtectedDataAndReportsLegacySocialAccount() {
        User user = seedLegacyUser("rotation@test.local", "회전 회원", "01012345678");
        User naverUser = userStorePort.save(User.fromSocialProfile(null, "네이버 회원"));
        Guest guest = guestRepository.save(new Guest(
                oldEncryptor.encrypt("회전 비회원"), oldIndexer.index("회전 비회원"),
                oldEncryptor.encrypt("01087654321"), oldIndexer.index("01087654321")));
        BookingClass bookingClass = classRepository.save(
                new BookingClass("키 회전 클래스", "KEY_ROTATION", 60, 50_000L, 0));
        Slot slot = slotRepository.save(new Slot(
                bookingClass,
                LocalDateTime.of(2030, 1, 2, 10, 0),
                LocalDateTime.of(2030, 1, 2, 11, 0)));
        Booking booking = bookingRepository.save(Booking.forMemberDeposit(
                user, slot, 5_000L, 45_000L, DepositPaymentMethod.CARD));
        jdbcTemplate.update(
                "UPDATE bookings SET owner_phone_hmac = ? WHERE id = ?",
                oldIndexer.index("01012345678"), booking.getId());
        String paymentPhone = "01022223333";
        PaymentAttempt attempt = paymentAttemptRepository.save(PaymentAttempt.startForGuest(
                UUID.randomUUID().toString(), PaymentContext.BOOKING, 10_000L,
                oldEncryptor.encrypt("""
                        {"type":"PREPARED_BOOKING","userId":null,"phone":"01022223333",
                         "guestVerificationProof":"v1.proof.signature","name":"회전 비회원","slotId":1,"passId":null,
                         "paymentMethod":"CARD","depositAmount":10000,"balanceAmount":90000}
                        """),
                oldIndexer.index(paymentPhone), "v1", "a".repeat(64)));
        LocalDateTime paidAt = LocalDateTime.of(2030, 1, 1, 10, 0);
        Order order = orderRepository.save(Order.forMember(
                user.getId(), 50_000L, paidAt, paidAt.plusHours(24)));
        Fulfillment fulfillment = fulfillmentRepository.save(Fulfillment.shipping(
                order.getId(), oldEncryptor.encrypt("{\"address\":\"서울\"}")));
        jdbcTemplate.update("""
                INSERT INTO shipping_address_changes(order_id, user_id, before_address_enc, after_address_enc, changed_at)
                VALUES (?, ?, ?, ?, ?)
                """, order.getId(), user.getId(), oldEncryptor.encrypt("이전 배송지"),
                oldEncryptor.encrypt("새 배송지"), paidAt);
        jdbcTemplate.update("""
                INSERT INTO smartstore_product_orders (
                    product_order_id, order_id, origin_product_no, product_name,
                    delivery_info_enc, product_order_status, initial_quantity, remain_quantity,
                    inventory_applied_quantity, last_changed_type, last_changed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "po-key-rotation", "order-key-rotation", 123L, "키 회전 상품",
                oldEncryptor.encrypt("{\"recipientName\":\"회전 수령인\"}"),
                "PAYED", 1, 1, 0, "PAYED", paidAt);
        PhoneVerification verification = new PhoneVerification(
                "01012345678", "123456",
                PhoneVerificationPurpose.GUEST_BOOKING, paidAt.plusMinutes(5));
        verification.protect(
                oldIndexer.index("01012345678"), oldIndexer.index("123456"),
                oldEncryptor.encrypt("123456"));
        phoneVerificationRepository.save(verification);
        EmailVerification emailVerification = new EmailVerification(
                naverUser.getId(),
                naverUser.getCredentialVersion(),
                "pending-email@example.com",
                "654321",
                paidAt.plusMinutes(5));
        emailVerification.protect(
                oldIndexer.index("pending-email@example.com"),
                oldIndexer.index(
                        naverUser.getId() + ":pending-email@example.com:654321"),
                oldEncryptor.encrypt("654321"));
        emailVerificationRepository.save(emailVerification);
        SocialAccount encryptedSocial = socialAccount(user.getId(), SocialProvider.NAVER,
                "naver-rotation-id", true);
        SocialAccount hmacOnlySocial = socialAccount(user.getId(), SocialProvider.GOOGLE,
                "google-legacy-id", false);
        AdminUser pendingAdmin = new AdminUser("rotation-admin", "password-hash");
        pendingAdmin.beginMfaEnrollment(oldEncryptor.encrypt("JBSWY3DPEHPK3PXP"));
        AdminUser admin = adminUserPort.save(pendingAdmin);

        jdbcTemplate.update("INSERT INTO group_inquiries(user_id, source, status, details_enc, created_at, updated_at) VALUES (?, 'WEBSITE', 'RECEIVED', ?, ?, ?)",
                user.getId(), oldEncryptor.encrypt("문의 연락처"), paidAt, paidAt);
        Long inquiryId = jdbcTemplate.queryForObject("SELECT id FROM group_inquiries WHERE user_id = ?", Long.class, user.getId());
        jdbcTemplate.update("INSERT INTO group_inquiry_activities(inquiry_id, to_status, note_enc, created_at) VALUES (?, 'RECEIVED', ?, ?)",
                inquiryId, oldEncryptor.encrypt("상담 메모"), paidAt);

        KeyRotationUseCase.RotationResult result = keyRotationUseCase.rotate("v1");

        assertSoftly(softly -> {
            softly.assertThat(result.users()).isEqualTo(2);
            softly.assertThat(result.guests()).isEqualTo(1);
            softly.assertThat(result.bookings()).isEqualTo(1);
            softly.assertThat(result.paymentAttempts()).isEqualTo(1);
            softly.assertThat(result.fulfillments()).isEqualTo(1);
            softly.assertThat(result.shippingAddressChanges()).isEqualTo(1);
            softly.assertThat(result.groupInquiries()).isEqualTo(1);
            softly.assertThat(result.groupInquiryActivities()).isEqualTo(1);
            softly.assertThat(value("group_inquiries", "details_enc", inquiryId)).startsWith("hg:v2:");
            softly.assertThat(jdbcTemplate.queryForObject("SELECT note_enc FROM group_inquiry_activities WHERE inquiry_id = ?", String.class, inquiryId)).startsWith("hg:v2:");
            var addressHistory = jdbcTemplate.queryForMap("SELECT before_address_enc, after_address_enc FROM shipping_address_changes WHERE order_id = ?", order.getId());
            softly.assertThat((String) addressHistory.get("before_address_enc")).startsWith("hg:v2:");
            softly.assertThat((String) addressHistory.get("after_address_enc")).startsWith("hg:v2:");
            softly.assertThat(result.smartStoreOrders()).isEqualTo(1);
            softly.assertThat(result.socialAccounts()).isEqualTo(1);
            softly.assertThat(result.adminMfaSecrets()).isEqualTo(1);
            softly.assertThat(result.deletedPhoneVerifications()).isEqualTo(1);
            softly.assertThat(result.deletedEmailVerifications()).isEqualTo(1);
            softly.assertThat(result.pendingSocialAccounts()).isEqualTo(1);
            softly.assertThat(result.pendingAdminMfaSecrets()).isZero();
            softly.assertThat(value("users", "email_enc", user.getId())).startsWith("hg:v2:");
            softly.assertThat(value("guests", "phone_enc", guest.getId())).startsWith("hg:v2:");
            softly.assertThat(value("bookings", "owner_phone_hmac", booking.getId()))
                    .isEqualTo(activeIndexKeyRing.index("01012345678"));
            softly.assertThat(value("payment_attempt", "payload_enc", attempt.getId())).startsWith("hg:v2:");
            softly.assertThat(value("payment_attempt", "owner_phone_hmac", attempt.getId()))
                    .isEqualTo(activeIndexKeyRing.index(paymentPhone));
            softly.assertThat(value("payment_attempt", "owner_phone_hmac_key_id", attempt.getId()))
                    .isEqualTo("v2");
            softly.assertThat(value("fulfillments", "shipping_address_enc", fulfillment.getId()))
                    .startsWith("hg:v2:");
            softly.assertThat(jdbcTemplate.queryForObject("""
                            SELECT delivery_info_enc
                            FROM smartstore_product_orders
                            WHERE product_order_id = 'po-key-rotation'
                            """, String.class))
                    .startsWith("hg:v2:");
            softly.assertThat(value("user_social_accounts", "provider_id_enc", encryptedSocial.getId()))
                    .startsWith("hg:v2:");
            softly.assertThat(value("user_social_accounts", "provider_id_enc", hmacOnlySocial.getId()))
                    .isNull();
            softly.assertThat(value("admin_user", "totp_secret_enc", admin.getId()))
                    .startsWith("hg:v2:");
            softly.assertThat(value("users", "email_hmac", user.getId()))
                    .isEqualTo(activeIndexKeyRing.index("rotation@test.local"));
            softly.assertThat(value("users", "email_enc", naverUser.getId())).isNull();
            softly.assertThat(value("users", "email_hmac", naverUser.getId())).isNull();
            softly.assertThat(value("user_social_accounts", "provider_id_hmac", encryptedSocial.getId()))
                    .isEqualTo(activeIndexKeyRing.index("naver-rotation-id"));
            softly.assertThat(value("user_social_accounts", "provider_id_hmac", hmacOnlySocial.getId()))
                    .isEqualTo(oldIndexer.index("google-legacy-id"));
            softly.assertThat(phoneVerificationRepository.count()).isZero();
            softly.assertThat(emailVerificationRepository.count()).isZero();
        });
        assertThat(activeEncryptor.decrypt(value("users", "email_enc", user.getId())))
                .isEqualTo("rotation@test.local");
    }

    @DisplayName("한 행이라도 복호화할 수 없으면 키 회전 전체를 롤백한다")
    @Test
    void rotate_corruptCiphertext_rollsBackAllChanges() {
        User first = seedLegacyUser("rotation-first@test.local", "첫 회원", "01011112222");
        User second = seedLegacyUser("rotation-second@test.local", "둘째 회원", "01033334444");
        String originalFirstCiphertext = value("users", "email_enc", first.getId());
        jdbcTemplate.update("UPDATE users SET email_enc = ? WHERE id = ?", "corrupt-ciphertext", second.getId());
        PhoneVerification verification = new PhoneVerification(
                "01011112222", "654321", PhoneVerificationPurpose.GUEST_BOOKING,
                LocalDateTime.of(2030, 1, 1, 10, 5));
        verification.protect(
                oldIndexer.index("01011112222"), oldIndexer.index("654321"),
                oldEncryptor.encrypt("654321"));
        phoneVerificationRepository.save(verification);

        assertThatThrownBy(() -> keyRotationUseCase.rotate("v1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("복호화");

        assertSoftly(softly -> {
            softly.assertThat(value("users", "email_enc", first.getId()))
                    .isEqualTo(originalFirstCiphertext)
                    .doesNotStartWith("hg:v2:");
            softly.assertThat(phoneVerificationRepository.count()).isEqualTo(1);
        });
    }

    private User seedLegacyUser(String email, String name, String phone) {
        User user = userStorePort.save(new User(email, "password-hash", name, phone));
        jdbcTemplate.update("""
                        UPDATE users
                        SET email_enc = ?, email_hmac = ?, name_enc = ?, name_hmac = ?,
                            phone_enc = ?, phone_hmac = ?
                        WHERE id = ?
                        """,
                oldEncryptor.encrypt(email), oldIndexer.index(email),
                oldEncryptor.encrypt(name), oldIndexer.index(name),
                oldEncryptor.encrypt(phone), oldIndexer.index(phone), user.getId());
        return user;
    }

    private SocialAccount socialAccount(Long userId, SocialProvider provider,
                                        String providerId, boolean encryptProviderId) {
        SocialAccount account = new SocialAccount(userId, provider, providerId);
        account.protect(
                encryptProviderId ? oldEncryptor.encrypt(providerId) : null,
                oldIndexer.index(providerId));
        return socialAccountRepository.save(account);
    }

    private String value(String table, String column, Long id) {
        return jdbcTemplate.queryForObject(
                "SELECT " + column + " FROM " + table + " WHERE id = ?", String.class, id);
    }

    private void cleanup() {
        cleanupSupport.clearOrderData();
        cleanupSupport.clearBookingWithPassAndRefundData();
        cleanupSupport.clearUsers();
        cleanupSupport.clearAdminUsers();
    }
}
