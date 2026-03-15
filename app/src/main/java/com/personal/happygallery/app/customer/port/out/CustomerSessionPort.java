package com.personal.happygallery.app.customer.port.out;

import com.personal.happygallery.domain.user.UserSession;
import java.util.Optional;

/**
 * 고객 세션 저장소 포트.
 *
 * <p>세션 저장 방식(DB, Redis, 인메모리 등)에 관계없이
 * application 서비스가 일관된 계약으로 세션을 관리할 수 있게 한다.
 */
public interface CustomerSessionPort {

    UserSession save(UserSession session);

    Optional<UserSession> findByTokenHash(String sessionTokenHash);

    void delete(UserSession session);
}
