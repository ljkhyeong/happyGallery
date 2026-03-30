package com.personal.happygallery.infra.time;

import com.personal.happygallery.domain.time.Clocks;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 서울 시간대 날짜 경계를 UTC {@link LocalDateTime} 반개구간으로 변환한다.
 *
 * <p>DB에는 UTC 기준 시각이 저장되고, 화면/API는 {@link Clocks#SEOUL} 기준 날짜를 입력받는 조회에 사용한다.
 * 이 변환으로 {@code [start, end)} 범위를 유지하면서 WHERE 절의 sargable 조건을 맞춘다.
 */
public final class SeoulDateTimeRangeConverter {

    private SeoulDateTimeRangeConverter() {}

    /** 서울 시간대 날짜의 자정(00:00)을 UTC LocalDateTime으로 변환 */
    public static LocalDateTime toUtcStart(LocalDate seoulDate) {
        return seoulDate.atStartOfDay(Clocks.SEOUL).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    /** 서울 시간대 날짜의 익일 자정(다음날 00:00)을 UTC LocalDateTime으로 변환 */
    public static LocalDateTime toUtcExclusiveEnd(LocalDate seoulDate) {
        return seoulDate.plusDays(1).atStartOfDay(Clocks.SEOUL).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
