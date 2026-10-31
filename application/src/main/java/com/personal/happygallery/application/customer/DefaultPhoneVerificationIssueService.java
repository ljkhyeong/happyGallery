package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.in.PhoneVerificationIssueUseCase;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationSender;
import com.personal.happygallery.domain.booking.PhoneVerification;
import com.personal.happygallery.domain.booking.PhoneVerificationPurpose;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultPhoneVerificationIssueService implements PhoneVerificationIssueUseCase {

    private static final Logger log = LoggerFactory.getLogger(DefaultPhoneVerificationIssueService.class);

    /** 인증 코드 유효 시간 (5분) */
    private static final int VERIFICATION_EXPIRE_MINUTES = 5;

    private final PhoneVerificationIssueTransactionService issueTransaction;
    private final PhoneVerificationSender phoneVerificationSender;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public DefaultPhoneVerificationIssueService(PhoneVerificationIssueTransactionService issueTransaction,
                                                PhoneVerificationSender phoneVerificationSender,
                                                Clock clock) {
        this.issueTransaction = issueTransaction;
        this.phoneVerificationSender = phoneVerificationSender;
        this.clock = clock;
    }

    /** 휴대폰 인증 코드를 생성·저장하고 SMS로 발송한다. 저장과 발송 완료는 각각 독립 트랜잭션이고 발송 자체는 트랜잭션 밖에서 수행한다. */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PhoneVerification sendVerificationCode(String phone, PhoneVerificationPurpose purpose) {
        String code = "%06d".formatted(random.nextInt(1_000_000));
        LocalDateTime expiresAt = LocalDateTime.now(clock)
                .plusMinutes(VERIFICATION_EXPIRE_MINUTES);
        PhoneVerification pv = new PhoneVerification(phone, code, purpose, expiresAt);
        pv = issueTransaction.save(pv);
        if (!phoneVerificationSender.send(pv.getPhone(), code)) {
            throw new HappyGalleryException(
                    ErrorCode.SERVICE_UNAVAILABLE,
                    "인증 코드를 발송하지 못했습니다. 잠시 후 다시 시도해주세요.");
        }
        pv = issueTransaction.completeDelivery(pv.getId(), pv.getPhone(), purpose);
        log.info("[phone-verification] created [verificationId={}]", pv.getId());
        return pv;
    }
}
