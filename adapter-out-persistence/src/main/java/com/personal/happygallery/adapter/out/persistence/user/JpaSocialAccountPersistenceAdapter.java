package com.personal.happygallery.adapter.out.persistence.user;

import com.personal.happygallery.application.customer.port.out.SocialAccountReaderPort;
import com.personal.happygallery.application.customer.port.out.SocialAccountStorePort;
import com.personal.happygallery.domain.crypto.BlindIndexer;
import com.personal.happygallery.domain.user.SocialAccount;
import com.personal.happygallery.domain.user.SocialProvider;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
class JpaSocialAccountPersistenceAdapter implements SocialAccountReaderPort, SocialAccountStorePort {

    private final SocialAccountRepository repository;
    private final BlindIndexer blindIndexer;

    JpaSocialAccountPersistenceAdapter(SocialAccountRepository repository, BlindIndexer blindIndexer) {
        this.repository = repository;
        this.blindIndexer = blindIndexer;
    }

    @Override
    public Optional<SocialAccount> findByProviderAndProviderId(
            SocialProvider provider, String providerId) {
        return repository.findByProviderAndProviderIdHmac(provider, index(providerId))
                .map(account -> restore(account, providerId));
    }

    @Override
    public boolean existsByUserIdAndProvider(Long userId, SocialProvider provider) {
        return repository.existsByUserIdAndProvider(userId, provider);
    }

    @Override
    public SocialAccount save(SocialAccount socialAccount) {
        String providerId = socialAccount.getProviderId();
        socialAccount.protect(index(providerId));
        return restore(repository.save(socialAccount), providerId);
    }

    private SocialAccount restore(SocialAccount account, String providerId) {
        account.restoreProviderId(providerId);
        return account;
    }

    private String index(String providerId) {
        if (!StringUtils.hasText(providerId)) {
            throw new IllegalArgumentException("providerId must not be blank");
        }
        return blindIndexer.index(providerId);
    }
}
