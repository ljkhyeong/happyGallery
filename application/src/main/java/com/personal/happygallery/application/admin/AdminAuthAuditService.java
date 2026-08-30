package com.personal.happygallery.application.admin;

import com.personal.happygallery.application.admin.port.out.AdminAuthHistoryPort;
import com.personal.happygallery.domain.admin.AdminAuthHistory;
import com.personal.happygallery.domain.admin.AdminAuthOutcome;
import com.personal.happygallery.domain.crypto.BlindIndexKeyRing;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AdminAuthAuditService {

    private final AdminAuthHistoryPort historyPort;
    private final BlindIndexKeyRing blindIndexKeyRing;
    private final Clock clock;

    AdminAuthAuditService(AdminAuthHistoryPort historyPort,
                          BlindIndexKeyRing blindIndexKeyRing,
                          Clock clock) {
        this.historyPort = historyPort;
        this.blindIndexKeyRing = blindIndexKeyRing;
        this.clock = clock;
    }

    @Transactional
    public void record(Long adminUserId, String subject, AdminAuthOutcome outcome) {
        historyPort.save(new AdminAuthHistory(
                adminUserId,
                subject == null ? null : blindIndexKeyRing.index(subject),
                subject == null ? null : blindIndexKeyRing.activeKeyId(),
                outcome,
                LocalDateTime.now(clock)));
    }
}
