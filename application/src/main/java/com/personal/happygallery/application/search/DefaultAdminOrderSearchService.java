package com.personal.happygallery.application.search;

import com.personal.happygallery.application.search.dto.AdminOrderSearchRow;
import com.personal.happygallery.application.search.port.in.AdminOrderSearchUseCase;
import com.personal.happygallery.application.search.port.out.AdminOrderSearchPort;
import com.personal.happygallery.application.search.port.out.AdminOrderSearchResult;
import com.personal.happygallery.application.shared.page.OffsetPage;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.domain.crypto.FieldEncryptor;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class DefaultAdminOrderSearchService implements AdminOrderSearchUseCase {

    private final AdminOrderSearchPort searchPort;
    private final FieldEncryptor fieldEncryptor;

    DefaultAdminOrderSearchService(AdminOrderSearchPort searchPort,
                                   FieldEncryptor fieldEncryptor) {
        this.searchPort = searchPort;
        this.fieldEncryptor = fieldEncryptor;
    }

    @Override
    public OffsetPage<AdminOrderSearchRow> search(OrderStatus status, LocalDate dateFrom, LocalDate dateTo,
                                                   String keyword, int page, int size) {
        return AdminSearchHelper.search(
                searchPort, status, dateFrom, dateTo, keyword, page, size, this::toResponse);
    }

    private AdminOrderSearchRow toResponse(AdminOrderSearchResult result) {
        return new AdminOrderSearchRow(
                result.orderId(),
                result.orderNumber(),
                result.status(),
                result.totalAmount(),
                fieldEncryptor.decrypt(result.buyerNameEnc()),
                decryptNullable(result.buyerPhoneEnc()),
                result.paidAt(),
                result.approvalDeadlineAt(),
                result.createdAt().atOffset(ZoneOffset.UTC));
    }

    private String decryptNullable(String encrypted) {
        return encrypted == null ? null : fieldEncryptor.decrypt(encrypted);
    }
}
