package com.personal.happygallery.adapter.out.persistence.user;

import com.personal.happygallery.application.customer.port.out.SocialAccountReaderPort;
import com.personal.happygallery.application.customer.port.out.SocialAccountStorePort;
import com.personal.happygallery.domain.crypto.BlindIndexKeyRing;
import com.personal.happygallery.domain.crypto.FieldEncryptor;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.user.SocialAccount;
import com.personal.happygallery.domain.user.SocialProvider;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
class JpaSocialAccountPersistenceAdapter implements SocialAccountReaderPort, SocialAccountStorePort {

    private static final String DUPLICATE_SOCIAL_IDENTITY_CONSTRAINT =
            "uq_user_social_accounts_provider_identity";
    private static final String DUPLICATE_SOCIAL_PROVIDER_CONSTRAINT =
            "uq_user_social_accounts_user_provider";

    private final SocialAccountRepository repository;
    private final FieldEncryptor fieldEncryptor;
    private final BlindIndexKeyRing blindIndexKeyRing;

    JpaSocialAccountPersistenceAdapter(SocialAccountRepository repository,
                                       FieldEncryptor fieldEncryptor,
                                       BlindIndexKeyRing blindIndexKeyRing) {
        this.repository = repository;
        this.fieldEncryptor = fieldEncryptor;
        this.blindIndexKeyRing = blindIndexKeyRing;
    }

    @Override
    public Optional<SocialAccount> findByProviderAndProviderId(
            SocialProvider provider, String providerId) {
        String requiredProviderId = requireProviderId(providerId);
        List<SocialAccount> matches = repository.findByProviderAndProviderIdHmacIn(
                provider, blindIndexKeyRing.indexCandidates(requiredProviderId));
        if (matches.size() > 1) {
            throw new IllegalStateException("소셜 계정 식별자가 키 후보 간 중복됩니다.");
        }
        return matches.stream()
                .findFirst()
                .map(account -> restoreAndBackfill(account, requiredProviderId));
    }

    @Override
    public Optional<SocialAccount> findByUserIdAndProvider(Long userId, SocialProvider provider) {
        return repository.findByUserIdAndProvider(userId, provider);
    }

    @Override
    public List<SocialAccount> findByUserId(Long userId) {
        return repository.findByUserIdOrderByProviderAsc(userId);
    }

    @Override
    public SocialAccount save(SocialAccount socialAccount) {
        String providerId = requireProviderId(socialAccount.getProviderId());
        protectWithActiveKeys(socialAccount, providerId);
        try {
            SocialAccount saved = repository.saveAndFlush(socialAccount);
            saved.restoreProviderId(providerId);
            return saved;
        } catch (DataIntegrityViolationException exception) {
            ErrorCode errorCode = socialConstraintErrorCode(exception);
            if (errorCode != null) {
                throw new HappyGalleryException(errorCode);
            }
            throw exception;
        }
    }

    @Override
    public void deleteByUserIdAndProvider(Long userId, SocialProvider provider) {
        repository.deleteByUserIdAndProvider(userId, provider);
    }

    @Override
    public void deleteByUserId(Long userId) {
        repository.deleteByUserId(userId);
    }

    private SocialAccount restoreAndBackfill(SocialAccount account, String providerId) {
        account.restoreProviderId(providerId);
        String activeHmac = blindIndexKeyRing.index(providerId);
        if (account.getProviderIdEnc() == null || !activeHmac.equals(account.getProviderIdHmac())) {
            protectWithActiveKeys(account, providerId);
            repository.save(account);
        }
        return account;
    }

    private void protectWithActiveKeys(SocialAccount account, String providerId) {
        account.protect(fieldEncryptor.encrypt(providerId), blindIndexKeyRing.index(providerId));
    }

    private String requireProviderId(String providerId) {
        if (!StringUtils.hasText(providerId)) {
            throw new IllegalArgumentException("providerId must not be blank");
        }
        return providerId;
    }

    private ErrorCode socialConstraintErrorCode(DataIntegrityViolationException exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof ConstraintViolationException violation
                    && StringUtils.hasText(violation.getConstraintName())) {
                String constraint = StringUtils.unqualify(violation.getConstraintName()
                        .toLowerCase(Locale.ROOT)
                        .replace("`", "")
                        .replace("\"", "")
                        .replace("'", ""));
                if (DUPLICATE_SOCIAL_IDENTITY_CONSTRAINT.equals(constraint)) {
                    return ErrorCode.SOCIAL_ACCOUNT_ALREADY_LINKED;
                }
                if (DUPLICATE_SOCIAL_PROVIDER_CONSTRAINT.equals(constraint)) {
                    return ErrorCode.SOCIAL_PROVIDER_ALREADY_LINKED;
                }
                return null;
            }
            current = current.getCause();
        }
        return null;
    }
}
