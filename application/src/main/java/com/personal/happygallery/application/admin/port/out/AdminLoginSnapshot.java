package com.personal.happygallery.application.admin.port.out;

/** BCrypt 사전 검증용 관리자 로그인 DTO. JPA 관리 대상이 아니다. */
public record AdminLoginSnapshot(
        Long adminUserId,
        String username,
        String passwordHash
) {
}
