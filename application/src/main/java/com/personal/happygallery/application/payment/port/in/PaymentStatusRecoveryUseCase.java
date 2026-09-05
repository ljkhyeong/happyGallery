package com.personal.happygallery.application.payment.port.in;

import com.personal.happygallery.application.payment.port.in.PaymentStatusQueryUseCase.CustomerPaymentStatus;
import com.personal.happygallery.domain.payment.PaymentContext;
import java.time.Instant;
import java.util.List;

/** 휴대폰 번호 인증으로 유실된 비회원 결제 상태 조회 자격과 orderId 목록을 복구한다. */
public interface PaymentStatusRecoveryUseCase {

    RecoveryResult recover(String phone, String verificationCode);

    record RecoveryResult(String statusToken, Instant expiresAt, List<RecoveredPayment> payments) {
        public RecoveryResult {
            payments = List.copyOf(payments);
        }
    }

    record RecoveredPayment(
            String orderId,
            PaymentContext context,
            long amount,
            CustomerPaymentStatus status
    ) {}
}
