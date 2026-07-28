package com.personal.happygallery.adapter.out.persistence.booking;

import com.personal.happygallery.application.customer.port.out.PhoneVerificationReaderPort;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationStorePort;
import com.personal.happygallery.domain.booking.PhoneVerification;
import com.personal.happygallery.domain.booking.PhoneVerificationPurpose;
import com.personal.happygallery.domain.crypto.BlindIndexer;
import com.personal.happygallery.domain.crypto.FieldEncryptor;
import com.personal.happygallery.domain.user.KoreanPhoneNumber;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class JpaPhoneVerificationPersistenceAdapter
        implements PhoneVerificationReaderPort, PhoneVerificationStorePort {

    private final PhoneVerificationRepository repository;
    private final FieldEncryptor fieldEncryptor;
    private final BlindIndexer blindIndexer;

    JpaPhoneVerificationPersistenceAdapter(PhoneVerificationRepository repository,
                                           FieldEncryptor fieldEncryptor,
                                           BlindIndexer blindIndexer) {
        this.repository = repository;
        this.fieldEncryptor = fieldEncryptor;
        this.blindIndexer = blindIndexer;
    }

    @Override
    public PhoneVerification save(PhoneVerification verification) {
        String phone = KoreanPhoneNumber.required(verification.getPhone());
        String code = verification.getCode();
        verification.protect(
                indexPhone(phone),
                indexCode(phone, verification.getPurpose(), code),
                fieldEncryptor.encrypt(code));
        return restore(repository.save(verification), phone);
    }

    @Override
    public Optional<PhoneVerification> findValidVerification(
            String phone,
            String code,
            PhoneVerificationPurpose purpose,
            LocalDateTime now) {
        String normalizedPhone = KoreanPhoneNumber.required(phone);
        return repository
                .findByPhoneHmacAndPurposeAndCodeHmacAndDeliveredTrueAndVerifiedFalseAndExpiresAtAfter(
                        indexPhone(normalizedPhone),
                        purpose,
                        indexCode(normalizedPhone, purpose, code),
                        now)
                .map(verification -> restore(verification, normalizedPhone));
    }

    @Override
    public Optional<PhoneVerification> findLatestUnverifiedCode(
            String phone,
            PhoneVerificationPurpose purpose) {
        String normalizedPhone = KoreanPhoneNumber.required(phone);
        return repository
                .findTopByPhoneHmacAndPurposeAndDeliveredTrueAndVerifiedFalseOrderByIdDesc(
                        indexPhone(normalizedPhone), purpose)
                .map(verification -> restore(verification, normalizedPhone));
    }

    @Override
    public Optional<PhoneVerification> findByIdForUpdate(
            Long verificationId,
            String phone,
            PhoneVerificationPurpose purpose) {
        String normalizedPhone = KoreanPhoneNumber.required(phone);
        return repository.findByIdAndPhoneHmacAndPurpose(
                        verificationId, indexPhone(normalizedPhone), purpose)
                .map(verification -> restore(verification, normalizedPhone));
    }

    @Override
    public void invalidateEarlierUnconsumedForPhone(
            String phone,
            PhoneVerificationPurpose purpose,
            Long verificationId) {
        repository.invalidateEarlierUnconsumedForPhone(
                indexPhone(KoreanPhoneNumber.required(phone)), purpose, verificationId);
    }

    @Override
    public int deleteExpiredBefore(LocalDateTime cutoff, int limit) {
        return repository.deleteExpiredBefore(cutoff, limit);
    }

    private PhoneVerification restore(PhoneVerification verification, String phone) {
        verification.restoreProtectedFields(phone, fieldEncryptor.decrypt(verification.getCodeEnc()));
        return verification;
    }

    private String indexPhone(String phone) {
        return blindIndexer.index(phone);
    }

    private String indexCode(
            String phone,
            PhoneVerificationPurpose purpose,
            String code) {
        return blindIndexer.index(phone + ":" + purpose.name() + ":" + code);
    }
}
