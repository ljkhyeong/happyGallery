package com.personal.happygallery.application.customer;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.user.User;

/** 보호된 계정 정보 변경 전에 요청 세션이 현재 인증 정보이고 최근 본인 확인을 거쳤는지 검사한다. */
final class ProtectedAccountChange {

    private ProtectedAccountChange() {}

    /** 잠근 회원 행의 인증 정보 버전과 비교하므로 회원 조회·잠금은 호출하는 쪽에서 먼저 한다. */
    static void requireCurrentSession(User user, long credentialVersion, boolean recentlyReauthenticated) {
        if (user.getCredentialVersion() != credentialVersion) {
            throw new HappyGalleryException(ErrorCode.UNAUTHORIZED);
        }
        if (!recentlyReauthenticated) {
            throw new HappyGalleryException(ErrorCode.REAUTHENTICATION_REQUIRED);
        }
    }
}
