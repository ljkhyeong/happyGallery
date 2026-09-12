package com.personal.happygallery.adapter.out.external.notification;

final class EmailVerificationMessage {

    private static final String MESSAGE_FORMAT = """
            해피갤러리 이메일 인증번호는 %s입니다.

            5분 안에 입력해 주세요. 본인이 요청하지 않았다면 이 메일을 무시해 주세요.
            """;

    private EmailVerificationMessage() {}

    static String body(String code) {
        return MESSAGE_FORMAT.formatted(code);
    }
}
