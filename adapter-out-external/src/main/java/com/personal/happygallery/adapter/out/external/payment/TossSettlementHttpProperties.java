package com.personal.happygallery.adapter.out.external.payment;

import com.personal.happygallery.adapter.out.external.http.HttpPool;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.external.payment.toss-settlement")
public record TossSettlementHttpProperties(
        @Valid @DefaultValue HttpPool http
) {
    private static final Duration MIN_RESPONSE_TIMEOUT = Duration.ofSeconds(60);

    /** 정산 조회는 응답이 느리므로 응답 timeout을 60초 이상으로 둔다. */
    @AssertTrue(message = "토스 정산 조회 응답 timeout은 60초 이상이어야 합니다.")
    public boolean isResponseTimeoutAtLeastOneMinute() {
        return http.timeout().compareTo(MIN_RESPONSE_TIMEOUT) >= 0;
    }
}
