package com.personal.happygallery.application.admin.port.out;

import com.personal.happygallery.domain.admin.AdminAuthHistory;
import java.time.LocalDateTime;

public interface AdminAuthHistoryPort {

    AdminAuthHistory save(AdminAuthHistory history);

    int deleteCreatedBefore(LocalDateTime cutoff, int limit);
}
