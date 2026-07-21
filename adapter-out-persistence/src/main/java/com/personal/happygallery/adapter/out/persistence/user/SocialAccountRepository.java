package com.personal.happygallery.adapter.out.persistence.user;

import com.personal.happygallery.domain.user.SocialAccount;
import com.personal.happygallery.domain.user.SocialProvider;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialAccountRepository
        extends JpaRepository<SocialAccount, Long> {

    List<SocialAccount> findByProviderAndProviderIdHmacIn(
            SocialProvider provider, Collection<String> providerIdHmacs);

    Optional<SocialAccount> findByUserIdAndProvider(Long userId, SocialProvider provider);

    List<SocialAccount> findByUserIdOrderByProviderAsc(Long userId);

    void deleteByUserIdAndProvider(Long userId, SocialProvider provider);

    void deleteByUserId(Long userId);
}
