package com.personal.happygallery.adapter.out.persistence.time;

import com.personal.happygallery.domain.time.Clocks;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 서울 날짜를 DB 열의 저장 기준에 맞는 {@link LocalDateTime} 반개구간으로 변환한다.
 *
 * <p>DB 기본값으로 생성되는 {@code created_at}은 UTC이고, 슬롯과 애플리케이션이 기록하는 업무 시각은
 * 서울 현지시각이다. 열의 저장 기준을 호출부가 명시적으로 선택하면서 {@code [start, end)}와
 * sargable 조건을 유지한다.
 */
public final class SeoulDateTimeRangeConverter {

    private SeoulDateTimeRangeConverter() {}

    public static LocalDateTime toLocalStart(LocalDate seoulDate) {
        return seoulDate.atStartOfDay();
    }

    public static LocalDateTime toLocalExclusiveEnd(LocalDate seoulDate) {
        return seoulDate.plusDays(1).atStartOfDay();
    }

    /** 서울 시간대 날짜의 자정(00:00)을 UTC LocalDateTime으로 변환 */
    public static LocalDateTime toUtcStart(LocalDate seoulDate) {
        return seoulDate.atStartOfDay(Clocks.SEOUL).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    /** 서울 시간대 날짜의 익일 자정(다음날 00:00)을 UTC LocalDateTime으로 변환 */
    public static LocalDateTime toUtcExclusiveEnd(LocalDate seoulDate) {
        return seoulDate.plusDays(1).atStartOfDay(Clocks.SEOUL).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
