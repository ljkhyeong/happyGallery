package com.personal.happygallery.adapter.out.external.http;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 외부 HTTP 커넥션풀 설정.
 *
 * <p>서비스별 기본값과 환경 변수는 {@code application.yml}에 둔다. 아래 기본값은 YAML 없이 바인딩하는
 * 경우에만 쓰인다.
 */
public record HttpPool(
        @NotNull @DurationMin(millis = 1) @DefaultValue("5s") Duration timeout,
        @NotNull @DurationMin(millis = 1) @DefaultValue("1s") Duration connectTimeout,
        @NotNull @DurationMin(millis = 1) @DefaultValue("500ms") Duration acquireTimeout,
        @Min(1) @DefaultValue("5") int maxConnections,
        @NotNull @DurationMin(millis = 1) @DefaultValue("30s") Duration keepAlive
) {

    /** 풀 대기·연결·응답 timeout을 모두 쓴 최악의 요청 시간. 바깥 TimeLimiter는 이보다 길어야 한다. */
    public Duration requestBudget() {
        return acquireTimeout.plus(connectTimeout).plus(timeout);
    }
}
