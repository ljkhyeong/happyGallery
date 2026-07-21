package com.personal.happygallery.application.customer.port.out;

import com.personal.happygallery.domain.user.SocialAccount;

public interface SocialAccountStorePort {

    SocialAccount save(SocialAccount socialAccount);

    void deleteByUserId(Long userId);
}
