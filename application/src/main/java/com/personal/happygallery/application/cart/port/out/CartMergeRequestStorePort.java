package com.personal.happygallery.application.cart.port.out;

import java.time.LocalDateTime;
import java.util.UUID;

public interface CartMergeRequestStorePort {

    enum Registration {
        RECORDED,
        REPLAY,
        CONFLICT
    }

    Registration register(Long userId, UUID idempotencyKey, String payloadHash,
                          LocalDateTime createdAt);

    int deleteCreatedBefore(LocalDateTime cutoff, int limit);
}
