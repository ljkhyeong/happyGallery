package com.personal.happygallery.adapter.out.persistence.user;

import com.personal.happygallery.application.customer.port.out.SocialAccountReaderPort;
import com.personal.happygallery.application.customer.port.out.SocialAccountStorePort;
import com.personal.happygallery.domain.user.SocialAccount;
import com.personal.happygallery.domain.user.SocialProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialAccountRepository
        extends JpaRepository<SocialAccount, Long>, SocialAccountReaderPort, SocialAccountStorePort {

    @Override
    Optional<SocialAccount> findByProviderAndProviderId(SocialProvider provider, String providerId);

    @Override
    Optional<SocialAccount> findByUserIdAndProvider(Long userId, SocialProvider provider);

    @Override
    SocialAccount save(SocialAccount socialAccount);
}
