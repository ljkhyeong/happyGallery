package com.personal.happygallery.application.cart;

import com.personal.happygallery.application.cart.port.in.CartUseCase.CartItemView;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/** 장바구니 화면과 결제 준비 요청을 연결하는 불투명 스냅샷 버전. */
final class CartSnapshotVersion {

    private static final char FIELD_SEPARATOR = '\u001f';

    private CartSnapshotVersion() {}

    static String from(List<CartItemView> items) {
        StringBuilder canonical = new StringBuilder();
        append(canonical, items.size());
        for (CartItemView item : items) {
            append(canonical, item.cartItemId());
            append(canonical, item.productId());
            append(canonical, item.productVariantId());
            append(canonical, item.productName());
            append(canonical, item.productType());
            append(canonical, item.basePrice());
            append(canonical, item.variantPriceAdjustment());
            append(canonical, item.textOptionPriceAdjustment());
            append(canonical, item.price());
            append(canonical, item.specification());
            append(canonical, item.careInstructions());
            append(canonical, item.productionLeadDays());
            append(canonical, item.options().size());
            item.options().forEach(option -> {
                append(canonical, option.type());
                append(canonical, option.groupName());
                append(canonical, option.value());
                append(canonical, option.priceAdjustment());
                append(canonical, option.sortOrder());
            });
            append(canonical, item.qty());
            append(canonical, item.available());
        }
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    sha256.digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }

    private static void append(StringBuilder canonical, Object value) {
        if (value == null) {
            canonical.append(-1).append(':').append(FIELD_SEPARATOR);
            return;
        }
        String text = value.toString();
        canonical.append(text.length()).append(':').append(text).append(FIELD_SEPARATOR);
    }
}
