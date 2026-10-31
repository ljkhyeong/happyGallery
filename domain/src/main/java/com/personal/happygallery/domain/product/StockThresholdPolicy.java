package com.personal.happygallery.domain.product;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;

public final class StockThresholdPolicy {
    private StockThresholdPolicy() {}

    public static void requireWritable(Integer minimumStock, long expectedVersion, long currentVersion) {
        if (minimumStock != null && minimumStock < 0) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "최소 보유 수량은 0 이상이어야 합니다.");
        }
        if (expectedVersion != currentVersion) {
            throw new HappyGalleryException(ErrorCode.CONFLICT, "재고 정보가 변경되었습니다. 최신 수량을 확인한 뒤 다시 저장해 주세요.");
        }
    }

    /** 재고 차감·복구 수량은 1 이상이어야 한다. */
    public static void requirePositiveQuantity(int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("재고 변경 수량은 1 이상이어야 합니다.");
        }
    }

    public static boolean isLow(boolean active, int quantity, Integer minimumStock) {
        return active && quantity <= (minimumStock == null ? 0 : minimumStock);
    }
}
