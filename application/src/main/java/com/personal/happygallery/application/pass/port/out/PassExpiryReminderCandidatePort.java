package com.personal.happygallery.application.pass.port.out;

import java.time.LocalDateTime;
import java.util.List;

public interface PassExpiryReminderCandidatePort {

    List<PassExpiryReminderTarget> findUnnotifiedExpiringAfterId(
            LocalDateTime now,
            LocalDateTime latestExpiry,
            int minimumCredits,
            Long afterId,
            int limit);
}
