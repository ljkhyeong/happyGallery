package com.personal.happygallery.domain.review;

/**
 * review 패키지 엔티티가 참조하는 저장 식별자 검증.
 *
 * <p>이미 영속된 후기·회원·관리자·증거 ID가 비거나 양수가 아니면 호출자 결함이므로 {@link IllegalArgumentException}을 던진다.
 * 사용자 입력 ID 검증은 {@code Review}가 INVALID_INPUT으로 따로 처리한다.
 */
final class ReviewIds {

    private ReviewIds() {}

    static Long requirePositive(Long value, String name) {
        if (value == null || value < 1L) {
            throw new IllegalArgumentException(name + "가 올바르지 않습니다.");
        }
        return value;
    }
}
