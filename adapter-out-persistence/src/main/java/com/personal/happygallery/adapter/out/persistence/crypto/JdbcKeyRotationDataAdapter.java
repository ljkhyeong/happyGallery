package com.personal.happygallery.adapter.out.persistence.crypto;

import com.personal.happygallery.application.crypto.rotation.KeyRotationDataPort;
import java.util.List;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcKeyRotationDataAdapter implements KeyRotationDataPort {

    private final JdbcClient jdbc;

    JdbcKeyRotationDataAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void acquireLock() {
        try {
            jdbc.sql("SELECT id FROM data_key_rotation_lock WHERE id = 1 FOR UPDATE NOWAIT")
                    .query(Integer.class)
                    .single();
        } catch (CannotAcquireLockException e) {
            throw new IllegalStateException("다른 키 회전이 실행 중입니다.", e);
        }
    }

    @Override
    public List<UserEncryptedRow> findUsersAfterId(long afterId, int limit) {
        return jdbc.sql("""
                        SELECT id, email_enc, name_enc, phone_enc, default_shipping_address_enc
                        FROM users
                        WHERE id > :afterId
                        ORDER BY id
                        LIMIT :limit
                        """)
                .param("afterId", afterId)
                .param("limit", limit)
                .query(UserEncryptedRow.class)
                .list();
    }

    @Override
    public void updateUser(UserRotatedRow row) {
        jdbc.sql("""
                        UPDATE users
                        SET email_enc = :emailEnc, email_hmac = :emailHmac,
                            name_enc = :nameEnc, name_hmac = :nameHmac,
                            phone_enc = :phoneEnc, phone_hmac = :phoneHmac,
                            default_shipping_address_enc = :defaultShippingAddressEnc
                        WHERE id = :id
                        """)
                .paramSource(row)
                .update();
    }

    @Override
    public List<GuestEncryptedRow> findGuestsAfterId(long afterId, int limit) {
        return jdbc.sql("""
                        SELECT id, name_enc, phone_enc
                        FROM guests
                        WHERE id > :afterId
                        ORDER BY id
                        LIMIT :limit
                        """)
                .param("afterId", afterId)
                .param("limit", limit)
                .query(GuestEncryptedRow.class)
                .list();
    }

    @Override
    public void updateGuest(GuestRotatedRow row) {
        jdbc.sql("""
                        UPDATE guests
                        SET name_enc = :nameEnc, name_hmac = :nameHmac,
                            phone_enc = :phoneEnc, phone_hmac = :phoneHmac
                        WHERE id = :id
                        """)
                .paramSource(row)
                .update();
    }

    @Override
    public int refreshBookedOwnerPhoneHmac() {
        return jdbc.sql("""
                        UPDATE bookings booking
                        LEFT JOIN users member ON member.id = booking.user_id
                        LEFT JOIN guests guest ON guest.id = booking.guest_id
                        SET booking.owner_phone_hmac =
                            CASE
                                WHEN booking.user_id IS NOT NULL THEN member.phone_hmac
                                ELSE guest.phone_hmac
                            END
                        WHERE booking.status = 'BOOKED'
                        """)
                .update();
    }

    @Override
    public List<PaymentAttemptEncryptedRow> findPaymentAttemptsAfterId(long afterId, int limit) {
        return jdbc.sql("""
                        SELECT id, payload_enc, fulfilled_access_token_enc AS access_token_enc,
                               owner_phone_hmac, owner_phone_hmac_key_id
                        FROM payment_attempt
                        WHERE id > :afterId
                          AND (payload_enc IS NOT NULL
                               OR fulfilled_access_token_enc IS NOT NULL
                               OR owner_phone_hmac IS NOT NULL)
                        ORDER BY id
                        LIMIT :limit
                        """)
                .param("afterId", afterId)
                .param("limit", limit)
                .query(PaymentAttemptEncryptedRow.class)
                .list();
    }

    @Override
    public void updatePaymentAttempt(PaymentAttemptRotatedRow row) {
        jdbc.sql("""
                        UPDATE payment_attempt
                        SET payload_enc = :payloadEnc,
                            fulfilled_access_token_enc = :accessTokenEnc,
                            owner_phone_hmac = :ownerPhoneHmac,
                            owner_phone_hmac_key_id = :ownerPhoneHmacKeyId
                        WHERE id = :id
                        """)
                .paramSource(row)
                .update();
    }

    @Override
    public List<FulfillmentEncryptedRow> findFulfillmentsAfterId(long afterId, int limit) {
        return jdbc.sql("""
                        SELECT id, shipping_address_enc
                        FROM fulfillments
                        WHERE id > :afterId
                          AND shipping_address_enc IS NOT NULL
                        ORDER BY id
                        LIMIT :limit
                        """)
                .param("afterId", afterId)
                .param("limit", limit)
                .query(FulfillmentEncryptedRow.class)
                .list();
    }

    @Override
    public void updateFulfillment(FulfillmentRotatedRow row) {
        jdbc.sql("""
                        UPDATE fulfillments
                        SET shipping_address_enc = :shippingAddressEnc
                        WHERE id = :id
                        """)
                .paramSource(row)
                .update();
    }

    @Override
    public List<ShippingAddressChangeRow> findShippingAddressChangesAfterId(long afterId, int limit) {
        return jdbc.sql("""
                        SELECT id, before_address_enc, after_address_enc FROM shipping_address_changes
                        WHERE id > :afterId ORDER BY id LIMIT :limit
                        """)
                .param("afterId", afterId).param("limit", limit)
                .query(ShippingAddressChangeRow.class).list();
    }

    @Override
    public void updateShippingAddressChange(ShippingAddressChangeRow row) {
        jdbc.sql("""
                        UPDATE shipping_address_changes
                        SET before_address_enc = :beforeAddressEnc, after_address_enc = :afterAddressEnc
                        WHERE id = :id
                        """).paramSource(row).update();
    }

    @Override
    public List<GroupInquiryEncryptedRow> findGroupInquiriesAfterId(long afterId, int limit) {
        return jdbc.sql("SELECT id, details_enc AS payload_enc FROM group_inquiries WHERE id > :afterId ORDER BY id LIMIT :limit")
                .param("afterId", afterId).param("limit", limit).query(GroupInquiryEncryptedRow.class).list();
    }

    @Override
    public void updateGroupInquiry(GroupInquiryEncryptedRow row) {
        jdbc.sql("UPDATE group_inquiries SET details_enc = :payloadEnc WHERE id = :id").paramSource(row).update();
    }

    @Override
    public List<GroupInquiryEncryptedRow> findGroupInquiryActivitiesAfterId(long afterId, int limit) {
        return jdbc.sql("SELECT id, note_enc AS payload_enc FROM group_inquiry_activities WHERE id > :afterId ORDER BY id LIMIT :limit")
                .param("afterId", afterId).param("limit", limit).query(GroupInquiryEncryptedRow.class).list();
    }

    @Override
    public void updateGroupInquiryActivity(GroupInquiryEncryptedRow row) {
        jdbc.sql("UPDATE group_inquiry_activities SET note_enc = :payloadEnc WHERE id = :id").paramSource(row).update();
    }

    @Override
    public List<SmartStoreOrderEncryptedRow> findSmartStoreOrdersAfterProductOrderId(
            String afterProductOrderId, int limit) {
        return jdbc.sql("""
                        SELECT product_order_id, delivery_info_enc
                        FROM smartstore_product_orders
                        WHERE product_order_id > :afterProductOrderId
                          AND delivery_info_enc IS NOT NULL
                        ORDER BY product_order_id
                        LIMIT :limit
                        """)
                .param("afterProductOrderId", afterProductOrderId)
                .param("limit", limit)
                .query(SmartStoreOrderEncryptedRow.class)
                .list();
    }

    @Override
    public void updateSmartStoreOrder(SmartStoreOrderRotatedRow row) {
        jdbc.sql("""
                        UPDATE smartstore_product_orders
                        SET delivery_info_enc = :deliveryInfoEnc
                        WHERE product_order_id = :productOrderId
                        """)
                .paramSource(row)
                .update();
    }

    @Override
    public List<SocialAccountEncryptedRow> findSocialAccountsAfterId(long afterId, int limit) {
        return jdbc.sql("""
                        SELECT id, provider_id_enc
                        FROM user_social_accounts
                        WHERE id > :afterId
                        ORDER BY id
                        LIMIT :limit
                        """)
                .param("afterId", afterId)
                .param("limit", limit)
                .query(SocialAccountEncryptedRow.class)
                .list();
    }

    @Override
    public void updateSocialAccount(SocialAccountRotatedRow row) {
        jdbc.sql("""
                        UPDATE user_social_accounts
                        SET provider_id_enc = :providerIdEnc,
                            provider_id_hmac = :providerIdHmac
                        WHERE id = :id
                        """)
                .paramSource(row)
                .update();
    }

    @Override
    public List<AdminTotpSecretRow> findAdminTotpSecretsAfterId(long afterId, int limit) {
        return jdbc.sql("""
                        SELECT id, totp_secret_enc
                        FROM admin_user
                        WHERE id > :afterId
                          AND totp_secret_enc IS NOT NULL
                        ORDER BY id
                        LIMIT :limit
                        """)
                .param("afterId", afterId)
                .param("limit", limit)
                .query(AdminTotpSecretRow.class)
                .list();
    }

    @Override
    public void updateAdminTotpSecret(AdminTotpSecretRow row) {
        jdbc.sql("""
                        UPDATE admin_user
                        SET totp_secret_enc = :totpSecretEnc
                        WHERE id = :id
                        """)
                .paramSource(row)
                .update();
    }

    @Override
    public long countAdminTotpSecretsNotWithKeyId(String keyId) {
        String keyPrefix = "hg:" + keyId + ":";
        return jdbc.sql("""
                        SELECT COUNT(*)
                        FROM admin_user
                        WHERE totp_secret_enc IS NOT NULL
                          AND LEFT(totp_secret_enc, CHAR_LENGTH(:keyPrefix)) <> :keyPrefix
                        """)
                .param("keyPrefix", keyPrefix)
                .query(Long.class)
                .single();
    }

    @Override
    public int deletePhoneVerifications() {
        return jdbc.sql("DELETE FROM phone_verifications").update();
    }

    @Override
    public int deleteEmailVerifications() {
        return jdbc.sql("DELETE FROM email_verifications").update();
    }

    @Override
    public long countSocialAccountsWithoutProviderIdEnc() {
        return jdbc.sql("SELECT COUNT(*) FROM user_social_accounts WHERE provider_id_enc IS NULL")
                .query(Long.class)
                .single();
    }
}
