package com.personal.happygallery.application.crypto.rotation;

import java.util.List;

public interface KeyRotationDataPort {

    interface IdentifiedRow {
        long id();
    }

    void acquireLock();

    List<UserEncryptedRow> findUsersAfterId(long afterId, int limit);

    void updateUser(UserRotatedRow row);

    List<GuestEncryptedRow> findGuestsAfterId(long afterId, int limit);

    void updateGuest(GuestRotatedRow row);

    int refreshBookedOwnerPhoneHmac();

    List<PaymentAttemptEncryptedRow> findPaymentAttemptsAfterId(long afterId, int limit);

    void updatePaymentAttempt(PaymentAttemptRotatedRow row);

    List<FulfillmentEncryptedRow> findFulfillmentsAfterId(long afterId, int limit);

    void updateFulfillment(FulfillmentRotatedRow row);

    List<ShippingAddressChangeRow> findShippingAddressChangesAfterId(long afterId, int limit);

    void updateShippingAddressChange(ShippingAddressChangeRow row);

    record ShippingAddressChangeRow(long id, String beforeAddressEnc, String afterAddressEnc)
            implements IdentifiedRow {}

    List<SmartStoreOrderEncryptedRow> findSmartStoreOrdersAfterProductOrderId(
            String afterProductOrderId, int limit);

    void updateSmartStoreOrder(SmartStoreOrderRotatedRow row);

    List<SocialAccountEncryptedRow> findSocialAccountsAfterId(long afterId, int limit);

    void updateSocialAccount(SocialAccountRotatedRow row);

    List<AdminTotpSecretRow> findAdminTotpSecretsAfterId(long afterId, int limit);

    void updateAdminTotpSecret(AdminTotpSecretRow row);

    long countAdminTotpSecretsNotWithKeyId(String keyId);

    int deletePhoneVerifications();

    int deleteEmailVerifications();

    long countSocialAccountsWithoutProviderIdEnc();

    record UserEncryptedRow(long id, String emailEnc, String nameEnc, String phoneEnc)
            implements IdentifiedRow {}

    record UserRotatedRow(long id, String emailEnc, String emailHmac,
                          String nameEnc, String nameHmac, String phoneEnc, String phoneHmac) {}

    record GuestEncryptedRow(long id, String nameEnc, String phoneEnc) implements IdentifiedRow {}

    record GuestRotatedRow(long id, String nameEnc, String nameHmac,
                           String phoneEnc, String phoneHmac) {}

    record PaymentAttemptEncryptedRow(
            long id, String payloadEnc, String accessTokenEnc,
            String ownerPhoneHmac, String ownerPhoneHmacKeyId)
            implements IdentifiedRow {}

    record PaymentAttemptRotatedRow(
            long id, String payloadEnc, String accessTokenEnc,
            String ownerPhoneHmac, String ownerPhoneHmacKeyId) {}

    record FulfillmentEncryptedRow(long id, String shippingAddressEnc) implements IdentifiedRow {}

    record FulfillmentRotatedRow(long id, String shippingAddressEnc) {}

    record SmartStoreOrderEncryptedRow(String productOrderId, String deliveryInfoEnc) {}

    record SmartStoreOrderRotatedRow(String productOrderId, String deliveryInfoEnc) {}

    record SocialAccountEncryptedRow(long id, String providerIdEnc) implements IdentifiedRow {}

    record SocialAccountRotatedRow(long id, String providerIdEnc, String providerIdHmac) {}

    record AdminTotpSecretRow(long id, String totpSecretEnc) implements IdentifiedRow {}
}
