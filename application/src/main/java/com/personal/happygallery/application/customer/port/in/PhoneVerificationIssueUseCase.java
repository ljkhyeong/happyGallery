package com.personal.happygallery.application.customer.port.in;

import com.personal.happygallery.domain.booking.PhoneVerification;
import com.personal.happygallery.domain.booking.PhoneVerificationPurpose;

/**
 * 목적별 휴대폰 인증 코드를 발급·저장하고 SMS로 발송한다.
 *
 * <p>비회원 예약·주문, 가입, 비밀번호 재설정, 회원 전화번호 등록·변경 등 모든 목적이 같은 발급 흐름을 쓴다.
 * 반환한 코드는 테스트·local 조회 용도이며 운영 응답에는 노출하지 않는다.
 */
public interface PhoneVerificationIssueUseCase {

    PhoneVerification sendVerificationCode(String phone, PhoneVerificationPurpose purpose);
}
