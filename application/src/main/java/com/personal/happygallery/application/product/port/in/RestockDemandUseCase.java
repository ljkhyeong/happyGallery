package com.personal.happygallery.application.product.port.in;

import com.personal.happygallery.application.shared.page.OffsetPage;

public interface RestockDemandUseCase {
    record Demand(Long productId, String productName, Long productVariantId, String optionLabel, long waitingCount) {}
    OffsetPage<Demand> list(Long productId, int page, int size);
}
