package com.personal.happygallery.domain.order;

import java.util.Arrays;
import java.util.Optional;

/** 배송조회 연동에서 사용하는 택배사 코드. */
public enum ShippingCarrier {
    CJ_LOGISTICS("cj", "CJ대한통운"),
    LOTTE("lotte", "롯데택배"),
    HANJIN("hanjin", "한진택배"),
    KOREA_POST("post", "우체국택배"),
    KYUNGDONG("kyungdong", "경동택배"),
    DAESIN("daesin", "대신택배"),
    LOGEN("logen", "로젠택배"),
    HAPDONG("hapdong", "합동택배"),
    COUPANG("coupang", "쿠팡로지스틱스"),
    WOORI("woori", "우리택배"),
    CU_POST("cupost", "CU 편의점택배"),
    GS_POSTBOX("gspostbox", "GS Postbox");

    private final String providerCode;
    private final String displayName;

    ShippingCarrier(String providerCode, String displayName) {
        this.providerCode = providerCode;
        this.displayName = displayName;
    }

    public String providerCode() {
        return providerCode;
    }

    public String displayName() {
        return displayName;
    }

    public static Optional<ShippingCarrier> fromProviderCode(String providerCode) {
        if (providerCode == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(carrier -> carrier.providerCode.equalsIgnoreCase(providerCode.strip()))
                .findFirst();
    }
}
