package com.personal.happygallery.application.pass.port.in;

import com.personal.happygallery.application.pass.port.in.PassRefundUseCase.PassRefundResult;

/** 회원이 본인 소유 8회권의 정산 환불을 요청한다. */
public interface MemberPassRefundUseCase {

    PassRefundResult refundMyPass(Long passId, Long userId);
}
