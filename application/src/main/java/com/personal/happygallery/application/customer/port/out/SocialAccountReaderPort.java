package com.personal.happygallery.application.customer.port.out;

import com.personal.happygallery.domain.user.SocialAccount;
import com.personal.happygallery.domain.user.SocialProvider;
import java.util.List;
import java.util.Optional;

public interface SocialAccountReaderPort {

    Optional<SocialAccount> findByProviderAndProviderId(SocialProvider provider, String providerId);

    Optional<SocialAccount> findByUserIdAndProvider(Long userId, SocialProvider provider);

    List<SocialAccount> findByUserId(Long userId);
}
