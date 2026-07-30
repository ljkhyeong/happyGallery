package com.personal.happygallery.domain.order;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;

/** 수령 방법과 배송지 조합 불변식. */
public final class FulfillmentPolicy {

    private FulfillmentPolicy() {}

    public static void requireValid(
            FulfillmentType fulfillmentType, ShippingAddress shippingAddress) {
        if (fulfillmentType == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "수령 방법을 선택해 주세요.");
        }
        if (fulfillmentType == FulfillmentType.SHIPPING && shippingAddress == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "배송지는 필수입니다.");
        }
        if (fulfillmentType == FulfillmentType.PICKUP && shippingAddress != null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "픽업 주문에는 배송지를 입력할 수 없습니다.");
        }
    }
}
