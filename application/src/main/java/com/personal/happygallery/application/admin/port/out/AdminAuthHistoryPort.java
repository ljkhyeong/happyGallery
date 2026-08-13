package com.personal.happygallery.application.admin.port.out;

import com.personal.happygallery.domain.admin.AdminAuthHistory;
import java.time.LocalDateTime;

public interface AdminAuthHistoryPort {

    <S extends AdminAuthHistory> S save(S history);

    int deleteCreatedBefore(LocalDateTime cutoff, int limit);
}
