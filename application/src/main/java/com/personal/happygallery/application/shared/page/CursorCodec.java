package com.personal.happygallery.application.shared.page;

import java.util.Base64;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 페이지 커서의 공통 직렬화.
 *
 * <p>값을 {@code |}로 이어 UTF-8 URL-safe Base64(패딩 없음)로 감싼다. 값 형식 검증과 오류 문구는
 * 커서별로 처리하고, 이 클래스는 형식이 어긋나면 {@link IllegalArgumentException}만 던진다.
 */
public final class CursorCodec {

    private static final String SEPARATOR = "|";
    private static final Pattern SEPARATOR_PATTERN = Pattern.compile(SEPARATOR, Pattern.LITERAL);
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private CursorCodec() {}

    public static String encode(String... values) {
        return encodeText(String.join(SEPARATOR, values));
    }

    /** 앞에서부터 {@code count}개 값으로 나눈다. 마지막 값에는 구분자가 남을 수 있다. */
    public static String[] decode(String cursor, int count) {
        String raw = new String(Base64.getUrlDecoder().decode(cursor), UTF_8);
        String[] values = SEPARATOR_PATTERN.split(raw, count);
        if (values.length != count) {
            throw new IllegalArgumentException("커서 값 개수가 맞지 않습니다.");
        }
        return values;
    }

    /** 구분자를 포함할 수 있는 값을 커서 값 하나로 넣을 때 감싼다. */
    public static String encodeText(String text) {
        return ENCODER.encodeToString(text.getBytes(UTF_8));
    }
}
