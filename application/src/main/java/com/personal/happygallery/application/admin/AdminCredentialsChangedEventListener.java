package com.personal.happygallery.application.admin;

import com.personal.happygallery.application.admin.port.out.AdminSessionPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class AdminCredentialsChangedEventListener {

    private static final Logger log =
            LoggerFactory.getLogger(AdminCredentialsChangedEventListener.class);

    private final AdminSessionPort sessionPort;

    AdminCredentialsChangedEventListener(AdminSessionPort sessionPort) {
        this.sessionPort = sessionPort;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void removeSessions(AdminCredentialsChangedEvent event) {
        try {
            sessionPort.removeAll(event.adminUserId(), event.invalidatedCredentialVersion());
        } catch (RuntimeException exception) {
            // credentialVersion 검증이 이전 세션을 막고, Redis 삭제는 저장 공간 정리 역할을 한다.
            log.error("관리자 비밀번호 변경 후 세션 삭제 실패 [adminUserId={} type={}]",
                    event.adminUserId(), exception.getClass().getSimpleName(), exception);
        }
    }
}

record AdminCredentialsChangedEvent(Long adminUserId, long invalidatedCredentialVersion) {}
