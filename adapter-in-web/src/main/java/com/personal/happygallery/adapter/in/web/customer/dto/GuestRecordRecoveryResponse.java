package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.application.customer.port.in.GuestRecordRecoveryUseCase.RecoveryResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** 비회원 기록 복구 응답. 복구한 주문·예약 목록은 커서 페이지 API로 조회한다. */
public record GuestRecordRecoveryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String accessToken,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OffsetDateTime expiresAt
) {
    public static GuestRecordRecoveryResponse from(RecoveryResult result) {
        return new GuestRecordRecoveryResponse(
                result.accessToken(),
                result.expiresAt().atOffset(ZoneOffset.UTC));
    }
}
