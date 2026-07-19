package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.application.customer.port.out.PhoneVerificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** 로컬·테스트 환경에서는 코드 원문을 남기지 않고 발송 성공만 모사한다. */
@Component
@Profile({"local", "dev", "test"})
public class FakePhoneVerificationSender implements PhoneVerificationSender {

    private static final Logger log = LoggerFactory.getLogger(FakePhoneVerificationSender.class);

    @Override
    public boolean send(String phone, String verificationCode) {
        log.info("[FAKE-SMS] purpose=PHONE_VERIFICATION");
        return true;
    }
}
