package com.personal.happygallery.application.pass.port.out;

/** 8회권 만료 리마인더 outbox 생성에 필요한 최소 수신자 정보. */
public record PassExpiryReminderTarget(Long passId, Long userId) {}
