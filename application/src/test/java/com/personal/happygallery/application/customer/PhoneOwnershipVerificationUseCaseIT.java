package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.booking.port.in.GuestBookingUseCase;
import com.personal.happygallery.application.customer.port.in.PhoneOwnershipVerificationUseCase;
import com.personal.happygallery.domain.error.PhoneVerificationFailedException;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@UseCaseIT
class PhoneOwnershipVerificationUseCaseIT {

    private static final String PHONE = "01012345678";

    @Autowired GuestBookingUseCase guestBookingUseCase;
    @Autowired PhoneOwnershipVerificationUseCase phoneOwnershipVerification;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired TestCleanupSupport cleanupSupport;

    @AfterEach
    void tearDown() {
        cleanupSupport.clearUsers();
    }

    @DisplayName("동일 인증 코드를 동시에 소비해도 한 요청만 성공한다")
    @Test
    void verify_concurrently_consumesCodeOnce() throws Exception {
        String code = guestBookingUseCase.sendVerificationCode(PHONE).getCode();
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> attempts = List.of(
                    executor.submit(() -> verifyAfter(start, code)),
                    executor.submit(() -> verifyAfter(start, code)));

            start.countDown();
            List<Boolean> results = List.of(
                    attempts.get(0).get(10, TimeUnit.SECONDS),
                    attempts.get(1).get(10, TimeUnit.SECONDS));

            assertThat(results).containsExactlyInAnyOrder(true, false);
        }
    }

    private boolean verifyAfter(CountDownLatch start, String code) throws InterruptedException {
        start.await();
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(
                    status -> phoneOwnershipVerification.verify(PHONE, code));
            return true;
        } catch (PhoneVerificationFailedException exception) {
            return false;
        }
    }
}
