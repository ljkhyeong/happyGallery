package com.personal.happygallery.application.admin;

import com.personal.happygallery.application.admin.port.in.AdminSetupUseCase;
import com.personal.happygallery.application.admin.port.out.AdminUserPort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.personal.happygallery.support.ConcurrentExecution.runSimultaneously;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@UseCaseIT
class AdminSetupConcurrencyUseCaseIT {

    @Autowired AdminSetupUseCase adminSetupUseCase;
    @Autowired AdminUserPort adminUserPort;
    @Autowired TestCleanupSupport cleanupSupport;

    @AfterEach
    void tearDown() {
        cleanupSupport.clearAdminUsers();
    }

    @DisplayName("최초 관리자 설정이 동시에 요청되어도 한 건만 생성된다")
    @Test
    void concurrentSetup_createsExactlyOneAdmin() throws Exception {
        List<Throwable> outcomes = runSimultaneously(List.of(
                () -> adminSetupUseCase.setup("first-admin", "admin123456"),
                () -> adminSetupUseCase.setup("second-admin", "admin123456")));

        assertSoftly(softly -> {
            softly.assertThat(outcomes).filteredOn(Objects::isNull).hasSize(1);
            softly.assertThat(outcomes)
                    .filteredOn(Objects::nonNull)
                    .singleElement()
                    .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                            softly.assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
            softly.assertThat(adminUserPort.count()).isEqualTo(1L);
        });
    }
}
