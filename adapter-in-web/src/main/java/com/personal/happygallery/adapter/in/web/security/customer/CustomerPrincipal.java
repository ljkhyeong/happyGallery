package com.personal.happygallery.adapter.in.web.security.customer;

import com.personal.happygallery.domain.user.User;
import java.security.Principal;

/** 요청 동안 응답에 필요한 회원 스냅샷을 보유하는 인증 주체. */
public record CustomerPrincipal(
        Long userId,
        String email,
        String name,
        String phone,
        boolean phoneVerified,
        boolean localPasswordEnabled,
        long credentialVersion
) implements Principal {

    public static CustomerPrincipal from(User user) {
        return new CustomerPrincipal(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.isPhoneVerified(),
                user.hasLocalPassword(),
                user.getCredentialVersion()
        );
    }

    @Override
    public String getName() {
        return userId.toString();
    }
}
