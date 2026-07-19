package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.customer.port.out.PhoneVerificationStorePort;
import com.personal.happygallery.domain.booking.PhoneVerification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** SMS 외부 호출 전에 인증 코드를 독립 트랜잭션으로 확정한다. */
@Service
public class PhoneVerificationIssueTransactionService {

    private final PhoneVerificationStorePort phoneVerificationStore;

    public PhoneVerificationIssueTransactionService(PhoneVerificationStorePort phoneVerificationStore) {
        this.phoneVerificationStore = phoneVerificationStore;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PhoneVerification save(PhoneVerification verification) {
        return phoneVerificationStore.save(verification);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PhoneVerification completeDelivery(Long verificationId, String phone) {
        PhoneVerification verification = phoneVerificationStore
                .findByIdForUpdate(verificationId, phone)
                .orElseThrow(() -> new IllegalStateException("발급한 휴대폰 인증 코드를 찾을 수 없습니다."));
        if (verification.isVerified()) {
            return verification;
        }

        phoneVerificationStore.invalidateEarlierUnconsumedForPhone(phone, verificationId);
        verification.markDelivered();
        return phoneVerificationStore.save(verification);
    }
}
