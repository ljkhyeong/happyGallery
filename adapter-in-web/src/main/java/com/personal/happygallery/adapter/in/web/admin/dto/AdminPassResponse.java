package com.personal.happygallery.adapter.in.web.admin.dto;

import static com.personal.happygallery.adapter.in.web.MaskingUtil.maskPhoneMiddle;

import com.personal.happygallery.application.search.dto.AdminPassStatus;
import com.personal.happygallery.application.search.dto.AdminPassView;
import com.personal.happygallery.application.shared.page.OffsetPage;
import com.personal.happygallery.domain.payment.RefundStatus;
import java.time.LocalDateTime;

public record AdminPassResponse(
        Long passId,
        String passNumber,
        String customerName,
        String customerPhone,
        AdminPassStatus status,
        int remainingCredits,
        int totalCredits,
        LocalDateTime expiresAt,
        int futureBookingCount,
        long expectedRefundAmount,
        RefundStatus refundStatus
) {

    public static AdminPassResponse from(AdminPassView pass) {
        return new AdminPassResponse(
                pass.passId(),
                pass.passNumber(),
                pass.customerName(),
                maskPhoneMiddle(pass.customerPhone()),
                pass.status(),
                pass.remainingCredits(),
                pass.totalCredits(),
                pass.expiresAt(),
                pass.futureBookingCount(),
                pass.expectedRefundAmount(),
                pass.refundStatus());
    }

    public static OffsetPage<AdminPassResponse> fromPage(OffsetPage<AdminPassView> passes) {
        return OffsetPage.of(
                passes.content().stream().map(AdminPassResponse::from).toList(),
                passes.page(),
                passes.size(),
                passes.totalCount());
    }
}
