package com.personal.happygallery.application.pass;

import com.personal.happygallery.application.pass.port.in.MemberPassRefundUseCase;
import com.personal.happygallery.application.pass.port.in.PassRefundUseCase;
import com.personal.happygallery.domain.error.PassExpiredException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultPassRefundService implements PassRefundUseCase, MemberPassRefundUseCase {

    private final PassRefundTransactionService transactionService;

    public DefaultPassRefundService(PassRefundTransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * 8회권 전체 환불. 관리자와 소유권 검증을 마친 회원 호출이 같은 정산 흐름을 사용한다.
     *
     * <ol>
     *   <li>미래 BOOKED 예약 자동 취소 (슬롯 booked_count--, 이력 기록)</li>
     *   <li>PG 환불 요청 이력 기록 및 커밋 이후 환불 실행 예약</li>
     *   <li>REFUND ledger 기록 (amount = 정산 환불 크레딧)</li>
     *   <li>remaining_credits = 0 (expire() 재활용)</li>
     * </ol>
     *
     * <p>PG 환불 실패 시 환불 이력은 비동기로 FAILED가 되어 운영자 재시도 대상이 된다.
     *
     * @return 처리 결과 (취소된 예약 수, 환불 크레딧, 환불 금액, 환불 이력)
     */
    @Override
    @Transactional(propagation = Propagation.NEVER)
    public PassRefundResult refundPass(Long passId) {
        return transactionService.refundIfActive(passId)
                .orElseThrow(PassExpiredException::new);
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public PassRefundResult refundMyPass(Long passId, Long userId) {
        return transactionService.refundOwnedIfActive(passId, userId)
                .orElseThrow(PassExpiredException::new);
    }
}
