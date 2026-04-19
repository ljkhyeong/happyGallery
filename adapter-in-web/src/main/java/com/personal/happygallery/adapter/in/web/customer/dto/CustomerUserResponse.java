package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.domain.user.User;

public record CustomerUserResponse(Long id, String email, String name, String phone,
                                    boolean phoneVerified, String provider) {

    public static CustomerUserResponse from(User user) {
        return new CustomerUserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.isPhoneVerified(),
                user.getProvider().name()
        );
    }
}
