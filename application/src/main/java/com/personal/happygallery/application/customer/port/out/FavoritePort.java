package com.personal.happygallery.application.customer.port.out;

import com.personal.happygallery.application.customer.port.in.FavoriteUseCase.View;
import com.personal.happygallery.domain.user.Favorite;
import com.personal.happygallery.domain.user.FavoriteTargetType;
import java.time.LocalDateTime;
import java.util.List;

public interface FavoritePort {
    Favorite save(Favorite favorite);
    boolean exists(Long userId, FavoriteTargetType type, Long targetId);
    void deleteTarget(Long userId, FavoriteTargetType type, Long targetId);
    void deleteByUserId(Long userId);
    List<View> list(Long userId, FavoriteTargetType type, LocalDateTime before, Long beforeId, int limit);
}
