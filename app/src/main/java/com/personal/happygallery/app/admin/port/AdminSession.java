package com.personal.happygallery.app.admin.port;

import java.time.Instant;

/** 관리자 인증 세션 정보. */
public record AdminSession(Long adminUserId, String username, Instant createdAt) {}
