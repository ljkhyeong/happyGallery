package com.personal.happygallery.adapter.out.persistence.booking;

import com.personal.happygallery.application.customer.port.out.PhoneVerificationReaderPort;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationStorePort;
import com.personal.happygallery.domain.booking.PhoneVerification;
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
        verification.protect(indexPhone(phone), indexCode(phone, code), fieldEncryptor.encrypt(code));
        return restore(repository.save(verification), phone);
    }

    @Override
    public Optional<PhoneVerification> findValidVerification(String phone, String code, LocalDateTime now) {
        String normalizedPhone = KoreanPhoneNumber.required(phone);
        return repository.findByPhoneHmacAndCodeHmacAndVerifiedFalseAndExpiresAtAfter(
                        indexPhone(normalizedPhone), indexCode(normalizedPhone, code), now)
                .map(verification -> restore(verification, normalizedPhone));
    }

    @Override
    public Optional<PhoneVerification> findLatestUnverifiedCode(String phone) {
        String normalizedPhone = KoreanPhoneNumber.required(phone);
        return repository.findTopByPhoneHmacAndVerifiedFalseOrderByIdDesc(indexPhone(normalizedPhone))
                .map(verification -> restore(verification, normalizedPhone));
    }

    private PhoneVerification restore(PhoneVerification verification, String phone) {
        verification.restoreProtectedFields(phone, fieldEncryptor.decrypt(verification.getCodeEnc()));
        return verification;
    }

    private String indexPhone(String phone) {
        return blindIndexer.index(phone);
    }

    private String indexCode(String phone, String code) {
        return blindIndexer.index(phone + ":" + code);
    }
}
