package com.personal.happygallery.application.qna;

import com.personal.happygallery.application.qna.port.out.SmartStoreInquiryProvider;
import com.personal.happygallery.application.shared.page.OffsetPage;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class SmartStoreInquirySearchTest {

    @Test
    @DisplayName("상품 문의는 선택일 전체를 조회하고 역전된 기간은 외부 호출 전에 거절한다")
    void selectedDates_includeWholeDaysAndRejectReversedRange() {
        SmartStoreInquiryProvider provider = mock(SmartStoreInquiryProvider.class);
        var service = new DefaultSmartStoreInquiryService(provider, Clock.systemUTC());
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 7, 31);
        when(provider.isEnabled()).thenReturn(true);
        when(provider.findProductInquiries(from.atStartOfDay(), to.atTime(23, 59, 59, 999_000_000), true, 2, 50))
                .thenReturn(new OffsetPage<>(List.of(), 2, 50, 0, 0));

        service.listPage(from, to, true, 2, 50);
        assertThatThrownBy(() -> service.listCustomerPage(to, from, false, 0, 50))
                .isInstanceOf(HappyGalleryException.class).hasMessage("조회 시작일은 종료일보다 늦을 수 없습니다.");
        verify(provider).findProductInquiries(from.atStartOfDay(), to.atTime(23, 59, 59, 999_000_000), true, 2, 50);
        verify(provider, times(2)).isEnabled();
        verifyNoMoreInteractions(provider);
    }
}
