package com.personal.happygallery.application.customer.port.in;

import com.personal.happygallery.application.shared.page.CursorPage;
import java.time.Instant;

/** 휴대폰 번호 인증을 거쳐 만료된 비회원 주문·예약 접근 권한을 복구한다. */
public interface GuestRecordRecoveryUseCase {

    RecoveryResult recover(String phone, String verificationCode);

    CursorPage<GuestOrderSummary> listRecoveredOrders(
            String accessToken, String cursor, int size);

    CursorPage<GuestBookingSummary> listRecoveredBookings(
            String accessToken, String cursor, int size);

    /** 새 복구 토큰. 복구한 주문·예약은 커서 페이지 조회로 읽는다. */
    record RecoveryResult(String accessToken, Instant expiresAt) {}
}
