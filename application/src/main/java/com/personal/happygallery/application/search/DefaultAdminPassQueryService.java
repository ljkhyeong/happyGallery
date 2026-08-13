package com.personal.happygallery.application.search;

import com.personal.happygallery.application.search.dto.AdminPassStatus;
import com.personal.happygallery.application.search.dto.AdminPassView;
import com.personal.happygallery.application.search.port.in.AdminPassQueryUseCase;
import com.personal.happygallery.application.search.port.out.AdminPassQueryPort;
import com.personal.happygallery.application.search.port.out.AdminPassQueryResult;
import com.personal.happygallery.application.shared.page.OffsetPage;
import com.personal.happygallery.application.shared.page.PageParams;
import com.personal.happygallery.domain.crypto.FieldEncryptor;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.payment.RefundStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class DefaultAdminPassQueryService implements AdminPassQueryUseCase {

    private final AdminPassQueryPort queryPort;
    private final FieldEncryptor fieldEncryptor;
    private final Clock clock;

    DefaultAdminPassQueryService(AdminPassQueryPort queryPort,
                                 FieldEncryptor fieldEncryptor,
                                 Clock clock) {
        this.queryPort = queryPort;
        this.fieldEncryptor = fieldEncryptor;
        this.clock = clock;
    }

    @Override
    public OffsetPage<AdminPassView> search(String keyword, int page, int size) {
        String safeKeyword = SearchParams.clampKeyword(keyword);
        int safePage = PageParams.clampPage(page);
        int safeSize = PageParams.clampSize(size);
        int offset = PageParams.offset(safePage, safeSize);
        long totalCount = queryPort.count(safeKeyword);
        if (totalCount == 0 || offset >= totalCount) {
            return OffsetPage.of(List.of(), safePage, safeSize, totalCount);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        List<AdminPassView> passes = queryPort.search(safeKeyword, now, offset, safeSize)
                .stream()
                .map(result -> toView(result, now))
                .toList();
        return OffsetPage.of(passes, safePage, safeSize, totalCount);
    }

    @Override
    public AdminPassView get(Long passId) {
        LocalDateTime now = LocalDateTime.now(clock);
        return queryPort.findById(passId, now)
                .map(result -> toView(result, now))
                .orElseThrow(NotFoundException.supplier("8회권"));
    }

    private AdminPassView toView(AdminPassQueryResult result, LocalDateTime now) {
        RefundStatus refundStatus = result.refundStatus() == null
                ? null
                : RefundStatus.valueOf(result.refundStatus());
        AdminPassStatus status = AdminPassStatus.from(
                refundStatus, result.expiresAt(), result.remainingCredits(), now);
        int refundableCredits = Math.clamp(
                result.remainingCredits() + result.futureBookingCount(), 0, result.totalCredits());
        long expectedRefundAmount = result.refundAmount() != null
                ? result.refundAmount()
                : calculateExpectedRefundAmount(result, status, refundableCredits);

        return new AdminPassView(
                result.passId(),
                result.passNumber(),
                fieldEncryptor.decrypt(result.customerNameEnc()),
                fieldEncryptor.decryptNullable(result.customerPhoneEnc()),
                status,
                result.remainingCredits(),
                result.totalCredits(),
                result.expiresAt(),
                result.futureBookingCount(),
                expectedRefundAmount,
                refundStatus);
    }

    private long calculateExpectedRefundAmount(AdminPassQueryResult result,
                                               AdminPassStatus status,
                                               int refundableCredits) {
        if (status == AdminPassStatus.EXPIRED) {
            return 0L;
        }
        return Math.multiplyExact(result.totalPrice(), refundableCredits) / result.totalCredits();
    }

}
