package com.personal.happygallery.adapter.out.external.notification;

/** NHN Cloud 알림톡·SMS 응답의 공통 header. 컴포넌트 이름은 응답 JSON 필드와 같다. */
record NhnResponseHeader(boolean isSuccessful, int resultCode) {

    /** 성공 플래그와 결과 코드 0을 함께 만족해야 응답 본문을 믿는다. */
    boolean ok() {
        return isSuccessful && resultCode == 0;
    }
}
