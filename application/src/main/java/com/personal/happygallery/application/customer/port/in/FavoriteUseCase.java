package com.personal.happygallery.application.customer.port.in;

import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.domain.user.FavoriteTargetType;
import java.time.LocalDateTime;

public interface FavoriteUseCase {
    record View(Long id, FavoriteTargetType targetType, Long targetId, String name, boolean active, LocalDateTime createdAt) {}
    void save(Long userId, FavoriteTargetType type, Long targetId);
    void remove(Long userId, FavoriteTargetType type, Long targetId);
    boolean isSaved(Long userId, FavoriteTargetType type, Long targetId);
    CursorPage<View> list(Long userId, FavoriteTargetType type, String cursor, int size);
}
