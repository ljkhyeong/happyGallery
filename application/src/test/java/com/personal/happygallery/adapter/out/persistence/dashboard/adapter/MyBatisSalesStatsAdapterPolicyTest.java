package com.personal.happygallery.adapter.out.persistence.dashboard.adapter;

import com.personal.happygallery.adapter.out.persistence.dashboard.mapper.SalesStatsMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@Tag("policy")
class MyBatisSalesStatsAdapterPolicyTest {

    @Test
    @DisplayName("개요 조회는 서울 업무 시각과 UTC 생성 시각의 날짜 경계를 구분한다")
    void findOverviewSplitsTodayRangeBySeoulAndUtcColumns() {
        SalesStatsMapper mapper = mock(SalesStatsMapper.class);
        MyBatisSalesStatsAdapter adapter = new MyBatisSalesStatsAdapter(mapper);

        adapter.findOverview(LocalDate.of(2026, 3, 5), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        ArgumentCaptor<LocalDateTime> rangeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(mapper).findOverview(
                rangeCaptor.capture(), rangeCaptor.capture(),
                rangeCaptor.capture(), rangeCaptor.capture(),
                rangeCaptor.capture(), rangeCaptor.capture());
        assertThat(rangeCaptor.getAllValues()).containsExactly(
                LocalDateTime.of(2026, 3, 5, 0, 0),
                LocalDateTime.of(2026, 3, 6, 0, 0),
                LocalDateTime.of(2026, 3, 4, 15, 0),
                LocalDateTime.of(2026, 3, 5, 15, 0),
                LocalDateTime.of(2026, 3, 1, 0, 0),
                LocalDateTime.of(2026, 4, 1, 0, 0));
    }
}
