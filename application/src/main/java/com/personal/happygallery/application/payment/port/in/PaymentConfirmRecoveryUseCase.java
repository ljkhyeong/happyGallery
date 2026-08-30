package com.personal.happygallery.application.payment.port.in;

import com.personal.happygallery.application.batch.BatchResult;

/** confirm 도중 중단된 결제를 기존 멱등 경로로 재개한다. */
public interface PaymentConfirmRecoveryUseCase {

    BatchResult recoverIncompleteConfirms();
}
