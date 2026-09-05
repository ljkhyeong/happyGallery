package com.personal.happygallery.application.product.port.out;

import com.personal.happygallery.domain.product.RestockAlert;
import java.util.List;
import java.util.Optional;

public interface RestockAlertPort {
    <S extends RestockAlert> S saveAndFlush(S alert);
    Optional<RestockAlert> findByIdForUpdate(Long id);
    Optional<RestockAlert> findByActiveKey(String key);
    List<RestockAlert> findByUserIdOrderByIdDesc(Long userId);
}
