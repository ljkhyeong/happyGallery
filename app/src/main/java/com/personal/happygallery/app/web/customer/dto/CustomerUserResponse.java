package com.personal.happygallery.app.web.customer.dto;

import com.personal.happygallery.domain.user.User;

public record CustomerUserResponse(Long id, String email, String name, String phone, boolean phoneVerified) {

    public static CustomerUserResponse from(User user) {
        return new CustomerUserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.isPhoneVerified()
        );
    }
}
