package com.personal.happygallery.application.admin;

import com.personal.happygallery.application.admin.port.in.AdminSetupUseCase;
import com.personal.happygallery.application.admin.port.out.AdminUserPort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

@UseCaseIT
class AdminSetupConcurrencyUseCaseIT {

    @Autowired AdminSetupUseCase adminSetupUseCase;
    @Autowired AdminUserPort adminUserPort;
    @Autowired TestCleanupSupport cleanupSupport;

    @BeforeEach
    void setUp() {
        cleanupSupport.clearAdminUsers();
    }

    @AfterEach
    void tearDown() {
        cleanupSupport.clearAdminUsers();
    }

    @DisplayName("최초 관리자 설정이 동시에 요청되어도 한 건만 생성된다")
    @Test
    void concurrentSetup_createsExactlyOneAdmin() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Throwable> first = executor.submit(
                    () -> runSetup(ready, start, "first-admin"));
            Future<Throwable> second = executor.submit(
                    () -> runSetup(ready, start, "second-admin"));

            ready.await(5, TimeUnit.SECONDS);
            start.countDown();
            Throwable firstResult = first.get(10, TimeUnit.SECONDS);
            Throwable secondResult = second.get(10, TimeUnit.SECONDS);
            Throwable failure = Stream.of(firstResult, secondResult)
                    .filter(result -> result != null)
                    .findFirst()
                    .orElse(null);

            assertSoftly(softly -> {
                softly.assertThat(Stream.of(firstResult, secondResult)
                                .filter(result -> result == null)
                                .count())
                        .isEqualTo(1);
                softly.assertThat(failure).isInstanceOf(HappyGalleryException.class);
                softly.assertThat(failure)
                        .extracting(result -> ((HappyGalleryException) result).getErrorCode())
                        .isEqualTo(ErrorCode.NOT_FOUND);
                softly.assertThat(adminUserPort.count()).isEqualTo(1L);
            });
        } finally {
            executor.shutdownNow();
        }
    }

    private Throwable runSetup(CountDownLatch ready, CountDownLatch start, String username) {
        ready.countDown();
        try {
            start.await();
            adminSetupUseCase.setup(username, "admin123456");
            return null;
        } catch (Throwable throwable) {
            return throwable;
        }
    }
}
