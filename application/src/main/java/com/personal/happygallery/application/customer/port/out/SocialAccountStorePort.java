package com.personal.happygallery.application.customer.port.out;

import com.personal.happygallery.domain.user.SocialAccount;
import com.personal.happygallery.domain.user.SocialProvider;

public interface SocialAccountStorePort {

    SocialAccount save(SocialAccount socialAccount);

    void deleteByUserIdAndProvider(Long userId, SocialProvider provider);

    void deleteByUserId(Long userId);
}
