package com.personal.happygallery.domain.order;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.text.DomainText;
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
        addressLine1 = DomainText.required(addressLine1, "기본 주소는", MAX_ADDRESS_LENGTH);
        addressLine2 = DomainText.optional(addressLine2, "상세 주소는", MAX_ADDRESS_LENGTH);
    }

    private static String requirePostalCode(String value) {
        String normalized = Objects.requireNonNullElse(value, "").strip();
        if (!POSTAL_CODE.matcher(normalized).matches()) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "우편번호는 숫자 5자리여야 합니다.");
        }
        return normalized;
    }
}
