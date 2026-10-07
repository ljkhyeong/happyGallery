package com.personal.happygallery.application.shared;

/**
 * 관리자 감사 기록에 남길 처리자다. 이름이 비어 있으면 "시스템"으로 기록한다.
 */
public record AdminActor(Long adminUserId, String name) {

    public AdminActor {
        name = name == null || name.isBlank() ? "시스템" : name.strip();
    }

    public static AdminActor system() {
        return new AdminActor(null, "시스템");
    }
}
