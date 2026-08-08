package com.personal.happygallery.adapter.in.web;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 공개 응답에서 개인정보를 마스킹하는 유틸리티.
 */
public final class MaskingUtil {

    private static final Pattern GRAPHEME_CLUSTER = Pattern.compile("\\X");

    private MaskingUtil() {}

    /** "홍길동" → "홍**" */
    public static String maskName(String name) {
        if (name == null) return "*";
        Matcher clusters = GRAPHEME_CLUSTER.matcher(name);
        if (!clusters.find()) return "*";

        String firstCluster = clusters.group();
        int remainingClusters = 0;
        while (clusters.find()) {
            remainingClusters++;
        }
        return remainingClusters == 0
                ? "*"
                : firstCluster + "*".repeat(remainingClusters);
    }

    /** "010-1234-5678" → "010****5678" (가운데 4자리 마스킹) */
    public static String maskPhoneMiddle(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
