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
import com.personal.happygallery.domain.pass.PassPurchase;
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
                .orElseThrow(NotFoundException.supplier("이용권"));
    }

    private AdminPassView toView(AdminPassQueryResult result, LocalDateTime now) {
        AdminPassStatus status = AdminPassStatus.from(
                result.refundStatus(), result.expiresAt(), result.remainingCredits(), now);
        long expectedRefundAmount = result.refundAmount() != null
                ? result.refundAmount()
                : calculateExpectedRefundAmount(result, status);

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
                result.refundStatus());
    }

    /** 실제 환불 처리와 같은 크레딧·금액 계산으로 미래 예약까지 취소했을 때의 환불액을 보여 준다. */
    private static long calculateExpectedRefundAmount(AdminPassQueryResult result, AdminPassStatus status) {
        if (status == AdminPassStatus.EXPIRED) {
            return 0L;
        }
        int refundableCredits = PassPurchase.refundableCredits(
                result.remainingCredits(), result.futureBookingCount(), result.totalCredits());
        return PassPurchase.refundAmount(result.totalPrice(), refundableCredits, result.totalCredits());
    }
}
