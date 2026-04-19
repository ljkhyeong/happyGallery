package com.personal.happygallery.application.admin.port.out;

import com.personal.happygallery.application.admin.port.AdminSession;
import java.util.Optional;

/**
 * 관리자 세션 저장소 포트.
 *
 * <p>세션 저장 방식(인메모리, Redis, DB 등)에 관계없이
 * application 서비스가 일관된 계약으로 관리자 세션을 관리할 수 있게 한다.
 */
public interface AdminSessionPort {

    /** 새 세션을 생성하고 토큰을 반환한다. */
    String create(Long adminUserId, String username);

    /** 토큰으로 세션을 조회·검증한다. 만료된 세션은 제거 후 empty를 반환한다. */
    Optional<AdminSession> validate(String token);

    /** 세션을 제거한다. */
    void remove(String token);
}
