package com.personal.happygallery.application.review.port.out;

import java.time.LocalDateTime;

public interface ReviewTombstoneRetentionPort {

    int deleteUnblockedBefore(LocalDateTime cutoff, int limit);
}
