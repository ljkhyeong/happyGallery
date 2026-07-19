package com.personal.happygallery.adapter.out.persistence.user;

import com.personal.happygallery.application.customer.port.out.SocialAccountReaderPort;
import com.personal.happygallery.application.customer.port.out.SocialAccountStorePort;
import com.personal.happygallery.domain.crypto.BlindIndexKeyRing;
import com.personal.happygallery.domain.crypto.FieldEncryptor;
import com.personal.happygallery.domain.user.SocialAccount;
import com.personal.happygallery.domain.user.SocialProvider;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
class JpaSocialAccountPersistenceAdapter implements SocialAccountReaderPort, SocialAccountStorePort {

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
    public SocialAccount save(SocialAccount socialAccount) {
        String providerId = requireProviderId(socialAccount.getProviderId());
        protectWithActiveKeys(socialAccount, providerId);
        SocialAccount saved = repository.save(socialAccount);
        saved.restoreProviderId(providerId);
        return saved;
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
}
