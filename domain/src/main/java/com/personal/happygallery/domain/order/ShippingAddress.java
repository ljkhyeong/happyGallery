package com.personal.happygallery.domain.order;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.user.KoreanPhoneNumber;
import com.personal.happygallery.domain.user.PersonalName;
import java.util.Objects;
import java.util.regex.Pattern;

/** 주문 시점에 확정하는 국내 배송지 스냅샷. */
public record ShippingAddress(
        String recipientName,
        String phone,
        String postalCode,
        String addressLine1,
        String addressLine2
) {

    private static final Pattern POSTAL_CODE = Pattern.compile("^[0-9]{5}$");
    private static final int MAX_ADDRESS_LENGTH = 200;

    public ShippingAddress {
        recipientName = PersonalName.required(recipientName);
        phone = KoreanPhoneNumber.required(phone);
        postalCode = requirePostalCode(postalCode);
        addressLine1 = requireAddress(addressLine1, "기본 주소");
        addressLine2 = optionalAddress(addressLine2);
    }

    private static String requirePostalCode(String value) {
        String normalized = Objects.requireNonNullElse(value, "").strip();
        if (!POSTAL_CODE.matcher(normalized).matches()) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "우편번호는 숫자 5자리여야 합니다.");
        }
        return normalized;
    }

    private static String requireAddress(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, fieldName + "는 필수입니다.");
        }
        return requireLength(value.strip(), fieldName);
    }

    private static String optionalAddress(String value) {
        return value == null || value.isBlank() ? null : requireLength(value.strip(), "상세 주소");
    }

    private static String requireLength(String value, String fieldName) {
        if (value.codePointCount(0, value.length()) > MAX_ADDRESS_LENGTH) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, fieldName + "는 " + MAX_ADDRESS_LENGTH + "자 이하여야 합니다.");
        }
        return value;
    }
}
