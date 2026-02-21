package com.personal.happygallery.domain.product;

import com.personal.happygallery.common.error.InventoryNotEnoughException;

/** 재고 정책. 단일 작품 중복 주문 방지 등 재고 차감 전 선행 검증을 담당한다. */
public final class InventoryPolicy {

    private InventoryPolicy() {}

    /**
     * 요청 수량만큼 재고가 있는지 확인한다.
     * 재고 부족 시 {@link InventoryNotEnoughException}을 던진다.
     */
    public static void checkSufficient(int available, int requested) {
        if (available < requested) {
            throw new InventoryNotEnoughException();
        }
    }
}
