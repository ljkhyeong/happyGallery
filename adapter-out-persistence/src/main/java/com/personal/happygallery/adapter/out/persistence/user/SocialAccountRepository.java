package com.personal.happygallery.adapter.out.persistence.user;

import com.personal.happygallery.domain.user.SocialAccount;
import com.personal.happygallery.domain.user.SocialProvider;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialAccountRepository
        extends JpaRepository<SocialAccount, Long> {

    List<SocialAccount> findByProviderAndProviderIdHmacIn(
            SocialProvider provider, Collection<String> providerIdHmacs);
}
