package com.personal.happygallery.adapter.out.persistence.user;

import com.personal.happygallery.application.customer.port.out.EmailVerificationReaderPort;
import com.personal.happygallery.application.customer.port.out.EmailVerificationStorePort;
import com.personal.happygallery.domain.crypto.BlindIndexer;
import com.personal.happygallery.domain.crypto.FieldEncryptor;
import com.personal.happygallery.domain.user.EmailAddress;
import com.personal.happygallery.domain.user.EmailVerification;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class JpaEmailVerificationPersistenceAdapter
        implements EmailVerificationReaderPort, EmailVerificationStorePort {

    private final EmailVerificationRepository repository;
    private final FieldEncryptor fieldEncryptor;
    private final BlindIndexer blindIndexer;

    JpaEmailVerificationPersistenceAdapter(
            EmailVerificationRepository repository,
            FieldEncryptor fieldEncryptor,
            BlindIndexer blindIndexer
    ) {
        this.repository = repository;
        this.fieldEncryptor = fieldEncryptor;
        this.blindIndexer = blindIndexer;
    }

    @Override
    public EmailVerification save(EmailVerification verification) {
        String email = EmailAddress.required(verification.getEmail());
        String code = verification.getCode();
        verification.protect(
                indexEmail(email),
                indexCode(verification.getUserId(), email, code),
                fieldEncryptor.encrypt(code));
        return restore(repository.save(verification), email);
    }

    @Override
    public Optional<EmailVerification> findValidVerification(
            Long userId,
            long credentialVersion,
            String email,
            String code,
            LocalDateTime now
    ) {
        String normalizedEmail = EmailAddress.required(email);
        return repository
                .findByUserIdAndCredentialVersionAndEmailHmacAndCodeHmacAndDeliveredTrueAndVerifiedFalseAndExpiresAtAfter(
                        userId,
                        credentialVersion,
                        indexEmail(normalizedEmail),
                        indexCode(userId, normalizedEmail, code),
                        now)
                .map(verification -> restore(verification, normalizedEmail));
    }

    @Override
    public Optional<EmailVerification> findLatestUnverifiedCode(Long userId, String email) {
        String normalizedEmail = EmailAddress.required(email);
        return repository
                .findTopByUserIdAndEmailHmacAndDeliveredTrueAndVerifiedFalseOrderByIdDesc(
                        userId, indexEmail(normalizedEmail))
                .map(verification -> restore(verification, normalizedEmail));
    }

    @Override
    public Optional<EmailVerification> findByIdForUpdate(
            Long verificationId,
            Long userId,
            long credentialVersion,
            String email
    ) {
        String normalizedEmail = EmailAddress.required(email);
        return repository.findByIdAndUserIdAndCredentialVersionAndEmailHmac(
                        verificationId,
                        userId,
                        credentialVersion,
                        indexEmail(normalizedEmail))
                .map(verification -> restore(verification, normalizedEmail));
    }

    @Override
    public void invalidateEarlierUnconsumed(Long userId, Long verificationId) {
        repository.invalidateEarlierUnconsumed(userId, verificationId);
    }

    @Override
    public int deleteExpiredBefore(LocalDateTime cutoff, int limit) {
        return repository.deleteExpiredBefore(cutoff, limit);
    }

    private EmailVerification restore(EmailVerification verification, String email) {
        verification.restoreProtectedFields(
                email,
                fieldEncryptor.decrypt(verification.getCodeEnc()));
        return verification;
    }

    private String indexEmail(String email) {
        return blindIndexer.index(email);
    }

    private String indexCode(Long userId, String email, String code) {
        return blindIndexer.index(userId + ":" + email + ":" + code);
    }
}
