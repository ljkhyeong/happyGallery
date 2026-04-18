package com.personal.happygallery.adapter.in.web;

/**
 * 공개 응답에서 개인정보를 마스킹하는 유틸리티.
 */
public final class MaskingUtil {

    private MaskingUtil() {}

    /** "홍길동" → "홍**" */
    public static String maskName(String name) {
        if (name == null || name.length() <= 1) return "*";
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }

    /** "010-1234-5678" → "010****5678" (가운데 4자리 마스킹) */
    public static String maskPhoneMiddle(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
