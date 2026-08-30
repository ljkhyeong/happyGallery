package com.personal.happygallery.application.admin;

import com.personal.happygallery.application.admin.port.out.AdminAuthHistoryPort;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuthHistoryRetentionService {

    private final AdminAuthHistoryPort historyPort;

    public AdminAuthHistoryRetentionService(AdminAuthHistoryPort historyPort) {
        this.historyPort = historyPort;
    }

    @Transactional
    public int deleteBatchBefore(LocalDateTime cutoff, int batchSize) {
        return historyPort.deleteCreatedBefore(cutoff, batchSize);
    }
}
