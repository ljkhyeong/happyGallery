package com.personal.happygallery.adapter.out.persistence.user;

import com.personal.happygallery.domain.user.SocialAccount;
import com.personal.happygallery.domain.user.SocialProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialAccountRepository
        extends JpaRepository<SocialAccount, Long> {

    Optional<SocialAccount> findByProviderAndProviderIdHmac(SocialProvider provider, String providerIdHmac);

    boolean existsByUserIdAndProvider(Long userId, SocialProvider provider);
}
