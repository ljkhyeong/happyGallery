package com.personal.happygallery.application.product.port.in;

import com.personal.happygallery.domain.product.RestockAlert;
import java.util.List;

public interface RestockAlertUseCase {
    record View(RestockAlert alert, String productName) {}
    RestockAlert register(Long userId, Long productId, Long productVariantId);
    List<View> list(Long userId);
    void cancel(Long userId, Long alertId);
}
