package com.personal.happygallery.application.crypto.rotation;

import com.personal.happygallery.application.crypto.VersionedFieldEncryptor;
import com.personal.happygallery.application.crypto.rotation.KeyRotationDataPort.ShippingAddressChangeRow;
import com.personal.happygallery.application.crypto.rotation.KeyRotationDataPort.AdminTotpSecretRow;
import com.personal.happygallery.application.crypto.rotation.KeyRotationDataPort.FulfillmentRotatedRow;
import com.personal.happygallery.application.crypto.rotation.KeyRotationDataPort.GuestRotatedRow;
import com.personal.happygallery.application.crypto.rotation.KeyRotationDataPort.IdentifiedRow;
import com.personal.happygallery.application.crypto.rotation.KeyRotationDataPort.PaymentAttemptEncryptedRow;
import com.personal.happygallery.application.crypto.rotation.KeyRotationDataPort.PaymentAttemptRotatedRow;
import com.personal.happygallery.application.crypto.rotation.KeyRotationDataPort.SocialAccountRotatedRow;
import com.personal.happygallery.application.crypto.rotation.KeyRotationDataPort.SmartStoreOrderRotatedRow;
import com.personal.happygallery.application.crypto.rotation.KeyRotationDataPort.UserRotatedRow;
import com.personal.happygallery.application.payment.context.PreparedPaymentPayload;
import com.personal.happygallery.application.payment.context.PreparedPaymentPayload.PreparedBookingPayload;
import com.personal.happygallery.application.payment.context.PreparedPaymentPayload.PreparedOrderPayload;
import com.personal.happygallery.domain.crypto.BlindIndexKeyRing;
import com.personal.happygallery.domain.user.KoreanPhoneNumber;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class DefaultKeyRotationService implements KeyRotationUseCase {

    private static final int PAGE_SIZE = 100;

    private final KeyRotationDataPort dataPort;
    private final VersionedFieldEncryptor fieldEncryptor;
    private final BlindIndexKeyRing blindIndexKeyRing;
    private final ObjectMapper objectMapper;

    public DefaultKeyRotationService(KeyRotationDataPort dataPort,
                                     VersionedFieldEncryptor fieldEncryptor,
                                     BlindIndexKeyRing blindIndexKeyRing,
                                     ObjectMapper objectMapper) {
        this.dataPort = dataPort;
        this.fieldEncryptor = fieldEncryptor;
        this.blindIndexKeyRing = blindIndexKeyRing;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(timeout = 600)
    public RotationResult rotate(String sourceKeyId) {
        requireRotationKeys(sourceKeyId);
        dataPort.acquireLock();
        int users = rotateUsers();
        int guests = rotateGuests();
        int bookings = dataPort.refreshBookedOwnerPhoneHmac();
        int paymentAttempts = rotatePaymentAttempts();
        int fulfillments = rotateFulfillments();
        int shippingAddressChanges = rotateShippingAddressChanges();
        int smartStoreOrders = rotateSmartStoreOrders();
        int socialAccounts = rotateSocialAccounts();
        int adminMfaSecrets = rotateAdminMfaSecrets();
        int deletedPhoneVerifications = dataPort.deletePhoneVerifications();
        int deletedEmailVerifications = dataPort.deleteEmailVerifications();
        long pendingSocialAccounts = dataPort.countSocialAccountsWithoutProviderIdEnc();
        long pendingAdminMfaSecrets =
                dataPort.countAdminTotpSecretsNotWithKeyId(fieldEncryptor.activeKeyId());
        if (pendingAdminMfaSecrets != 0) {
            throw new IllegalStateException(
                    "구 키로 암호화된 관리자 MFA 비밀키가 남아 있습니다: " + pendingAdminMfaSecrets);
        }
        return new RotationResult(users, guests, bookings, paymentAttempts, fulfillments, shippingAddressChanges,
                smartStoreOrders,
                socialAccounts, adminMfaSecrets,
                deletedPhoneVerifications, deletedEmailVerifications,
                pendingSocialAccounts, pendingAdminMfaSecrets);
    }

    private int rotateUsers() {
        return rotatePages((afterId, limit) -> dataPort.findUsersAfterId(afterId, limit), row -> {
            String email = fieldEncryptor.decryptNullable(row.emailEnc());
            String name = fieldEncryptor.decrypt(row.nameEnc());
            String phone = fieldEncryptor.decryptNullable(row.phoneEnc());
            dataPort.updateUser(new UserRotatedRow(
                    row.id(), encryptNullable(email),
                    email == null ? null : blindIndexKeyRing.index(email),
                    fieldEncryptor.encrypt(name), blindIndexKeyRing.index(name),
                    phone == null ? null : fieldEncryptor.encrypt(phone),
                    phone == null ? null : blindIndexKeyRing.index(phone)));
            return true;
        });
    }

    private int rotateGuests() {
        return rotatePages((afterId, limit) -> dataPort.findGuestsAfterId(afterId, limit), row -> {
            String name = fieldEncryptor.decrypt(row.nameEnc());
            String phone = fieldEncryptor.decrypt(row.phoneEnc());
            dataPort.updateGuest(new GuestRotatedRow(
                    row.id(), fieldEncryptor.encrypt(name), blindIndexKeyRing.index(name),
                    fieldEncryptor.encrypt(phone), blindIndexKeyRing.index(phone)));
            return true;
        });
    }

    private int rotatePaymentAttempts() {
        return rotatePages((afterId, limit) -> dataPort.findPaymentAttemptsAfterId(afterId, limit), row -> {
            String payloadJson = fieldEncryptor.decryptNullable(row.payloadEnc());
            dataPort.updatePaymentAttempt(new PaymentAttemptRotatedRow(
                    row.id(),
                    encryptNullable(payloadJson),
                    row.accessTokenEnc() == null
                            ? null
                            : fieldEncryptor.reencrypt(row.accessTokenEnc()),
                    rotateOwnerPhoneHmac(row.ownerPhoneHmac(), payloadJson),
                    rotateOwnerPhoneHmacKeyId(row, payloadJson)));
            return true;
        });
    }

    private String rotateOwnerPhoneHmac(String ownerPhoneHmac, String payloadJson) {
        if (ownerPhoneHmac == null) {
            return null;
        }
        if (payloadJson == null) {
            return ownerPhoneHmac;
        }
        PreparedPaymentPayload payload =
                objectMapper.readValue(payloadJson, PreparedPaymentPayload.class);
        String phone = switch (payload) {
            case PreparedOrderPayload order -> order.phone();
            case PreparedBookingPayload booking -> booking.phone();
            default -> throw new IllegalStateException("비회원 결제 휴대폰을 찾을 수 없는 payload입니다.");
        };
        return blindIndexKeyRing.index(KoreanPhoneNumber.required(phone));
    }

    private String rotateOwnerPhoneHmacKeyId(
            PaymentAttemptEncryptedRow row, String payloadJson) {
        if (row.ownerPhoneHmac() == null) {
            return null;
        }
        return payloadJson == null ? row.ownerPhoneHmacKeyId() : blindIndexKeyRing.activeKeyId();
    }

    private int rotateFulfillments() {
        return rotatePages((afterId, limit) -> dataPort.findFulfillmentsAfterId(afterId, limit), row -> {
            dataPort.updateFulfillment(new FulfillmentRotatedRow(
                    row.id(), fieldEncryptor.reencrypt(row.shippingAddressEnc())));
            return true;
        });
    }

    private int rotateShippingAddressChanges() {
        return rotatePages(dataPort::findShippingAddressChangesAfterId, row -> {
            dataPort.updateShippingAddressChange(new ShippingAddressChangeRow(row.id(),
                    fieldEncryptor.reencrypt(row.beforeAddressEnc()), fieldEncryptor.reencrypt(row.afterAddressEnc())));
            return true;
        });
    }

    private int rotateSmartStoreOrders() {
        String afterProductOrderId = "";
        int rotated = 0;
        while (true) {
            var page = dataPort.findSmartStoreOrdersAfterProductOrderId(
                    afterProductOrderId, PAGE_SIZE);
            if (page.isEmpty()) {
                return rotated;
            }
            for (var row : page) {
                dataPort.updateSmartStoreOrder(new SmartStoreOrderRotatedRow(
                        row.productOrderId(), fieldEncryptor.reencrypt(row.deliveryInfoEnc())));
                rotated++;
                afterProductOrderId = row.productOrderId();
            }
        }
    }

    private int rotateSocialAccounts() {
        return rotatePages((afterId, limit) -> dataPort.findSocialAccountsAfterId(afterId, limit), row -> {
            if (row.providerIdEnc() == null) {
                return false;
            }
            String providerId = fieldEncryptor.decrypt(row.providerIdEnc());
            dataPort.updateSocialAccount(new SocialAccountRotatedRow(
                    row.id(), fieldEncryptor.encrypt(providerId),
                    blindIndexKeyRing.index(providerId)));
            return true;
        });
    }

    private int rotateAdminMfaSecrets() {
        return rotatePages(
                (afterId, limit) -> dataPort.findAdminTotpSecretsAfterId(afterId, limit),
                row -> {
                    dataPort.updateAdminTotpSecret(new AdminTotpSecretRow(
                            row.id(), fieldEncryptor.reencrypt(row.totpSecretEnc())));
                    return true;
                });
    }

    private void requireRotationKeys(String sourceKeyId) {
        if (sourceKeyId == null || sourceKeyId.isBlank()) {
            throw new IllegalArgumentException("회전 원본 키 ID는 필수입니다.");
        }
        if (fieldEncryptor.activeKeyId().equals(sourceKeyId)
                || blindIndexKeyRing.activeKeyId().equals(sourceKeyId)) {
            throw new IllegalArgumentException("원본 키 ID와 활성 키 ID는 달라야 합니다.");
        }
        if (!fieldEncryptor.keyIds().contains(sourceKeyId)
                || !blindIndexKeyRing.keyIds().contains(sourceKeyId)) {
            throw new IllegalArgumentException("원본 AES/HMAC 키가 previous 키링에 모두 필요합니다: " + sourceKeyId);
        }
        if (!fieldEncryptor.activeKeyId().equals(blindIndexKeyRing.activeKeyId())) {
            throw new IllegalStateException("AES와 HMAC 활성 키 ID가 일치해야 합니다.");
        }
    }

    private String encryptNullable(String plaintext) {
        return plaintext == null ? null : fieldEncryptor.encrypt(plaintext);
    }

    private static <T extends IdentifiedRow> int rotatePages(PageReader<T> reader, RowRotator<T> rotator) {
        long afterId = 0L;
        int rotated = 0;
        while (true) {
            var page = reader.read(afterId, PAGE_SIZE);
            if (page.isEmpty()) {
                return rotated;
            }
            for (T row : page) {
                if (rotator.rotate(row)) {
                    rotated++;
                }
                afterId = row.id();
            }
        }
    }

    @FunctionalInterface
    private interface PageReader<T extends IdentifiedRow> {
        List<T> read(long afterId, int limit);
    }

    @FunctionalInterface
    private interface RowRotator<T> {
        boolean rotate(T row);
    }
}
