package com.personal.happygallery.application.admin.port.out;

/** BCrypt 사전 검증용 비관리 관리자 로그인 값. */
public record AdminLoginSnapshot(
        Long adminUserId,
        String username,
        String passwordHash
) {
}
