package com.personal.happygallery.application.payment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.personal.happygallery.application.store.port.in.WorkshopProfileUseCase;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.store.WorkshopProfile;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PublicPaymentAvailabilityGuardTest {

    @DisplayName("운영 결제는 온라인 판매 고지 정보가 모두 입력된 뒤에만 연다")
    @Test
    void requireAvailable_requiresCompleteBusinessProfile() {
        WorkshopProfileUseCase profileUseCase = mock(WorkshopProfileUseCase.class);
        WorkshopProfile profile = new WorkshopProfile("해피갤러리");
        when(profileUseCase.get()).thenReturn(profile);
        PublicPaymentAvailabilityGuard guard = new PublicPaymentAvailabilityGuard(
                new PublicPaymentProperties(true), profileUseCase);

        assertThatThrownBy(guard::requireAvailable)
                .isInstanceOf(HappyGalleryException.class)
                .hasMessageContaining("온라인 결제를 준비 중");

        profile.update(
                "해피갤러리", "010-9635-5608", null,
                "충북 충주시 계명대로 161", "1층", null, null, null,
                "303-11-87052", "대표자", "owner@example.com", "신고번호",
                null, "ssim1972", true, LocalDateTime.of(2026, 7, 21, 10, 0));

        assertThatCode(guard::requireAvailable).doesNotThrowAnyException();
    }
}
