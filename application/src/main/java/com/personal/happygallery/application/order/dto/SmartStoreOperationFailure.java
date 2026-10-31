package com.personal.happygallery.application.order.dto;

/** 스마트스토어 일괄 처리에서 실패한 상품 주문과 실패 코드·사유다. */
public record SmartStoreOperationFailure(String productOrderId, String code, String message) {

    /** 네이버 응답에서 해당 상품 주문의 처리 결과를 확인할 수 없을 때 adapter가 붙이는 실패 코드. */
    public static final String UNKNOWN_RESULT_CODE = "UNKNOWN_RESULT";

    /** 거절이 아니라 처리 결과를 알 수 없는 실패인지 돌려준다. 호출자는 결과 미확인으로 기록한다. */
    public boolean isResultUnknown() {
        return UNKNOWN_RESULT_CODE.equals(code);
    }
}
