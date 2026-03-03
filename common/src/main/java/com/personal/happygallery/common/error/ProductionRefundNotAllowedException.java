package com.personal.happygallery.common.error;

/** 제작 중인 주문에 대한 환불/취소 시도 시 발생 (§8.3). */
public class ProductionRefundNotAllowedException extends HappyGalleryException {

    public ProductionRefundNotAllowedException() {
        super(ErrorCode.PRODUCTION_REFUND_NOT_ALLOWED);
    }
}
