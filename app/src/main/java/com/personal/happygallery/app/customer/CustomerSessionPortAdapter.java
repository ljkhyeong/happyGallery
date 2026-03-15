package com.personal.happygallery.app.customer;

import com.personal.happygallery.app.customer.port.out.CustomerSessionPort;
import com.personal.happygallery.domain.user.UserSession;
import com.personal.happygallery.infra.user.UserSessionRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * {@link UserSessionRepository}(infra) → {@link CustomerSessionPort}(app) 브릿지 어댑터.
 *
 * <p>점진적 헥사고날 전환 중 app 서비스가 infra repository를 직접 참조하지 않도록 중개한다.
 * 향후 infra 모듈이 app 포트를 직접 구현하게 되면 이 클래스는 제거된다.
 */
@Component
class CustomerSessionPortAdapter implements CustomerSessionPort {

    private final UserSessionRepository sessionRepository;

    CustomerSessionPortAdapter(UserSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Override
    public UserSession save(UserSession session) {
        return sessionRepository.save(session);
    }

    @Override
    public Optional<UserSession> findByTokenHash(String sessionTokenHash) {
        return sessionRepository.findBySessionTokenHash(sessionTokenHash);
    }

    @Override
    public void delete(UserSession session) {
        sessionRepository.delete(session);
    }
}
