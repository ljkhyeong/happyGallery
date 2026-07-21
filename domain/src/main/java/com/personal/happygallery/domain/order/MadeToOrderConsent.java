package com.personal.happygallery.domain.order;

import java.time.LocalDateTime;

/** 주문제작 상품의 청약철회 제한 안내와 고객 동의 스냅샷. */
public record MadeToOrderConsent(String version, String disclosure, LocalDateTime agreedAt) {

    public static final String CURRENT_VERSION = "2026-07-21-v1";
    public static final String CURRENT_DISCLOSURE =
            "주문제작 상품은 결제 후 관리자 승인으로 제작이 시작되면 다른 고객에게 판매하기 어려워 "
                    + "청약철회가 제한될 수 있음을 확인하고 동의합니다. 다만 하자, 오배송 등 법령상 권리는 제한되지 않습니다.";

    public static MadeToOrderConsent current(LocalDateTime agreedAt) {
        return new MadeToOrderConsent(CURRENT_VERSION, CURRENT_DISCLOSURE, agreedAt);
    }
}
