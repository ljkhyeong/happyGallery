package com.personal.happygallery.application.search;

import com.personal.happygallery.application.customer.GuestPhoneProtector;
import com.personal.happygallery.application.search.dto.AdminOrderSearchRow;
import com.personal.happygallery.application.search.port.in.AdminOrderSearchUseCase;
import com.personal.happygallery.application.search.port.out.AdminOrderSearchPort;
import com.personal.happygallery.application.search.port.out.AdminOrderSearchResult;
import com.personal.happygallery.application.shared.page.OffsetPage;
import com.personal.happygallery.domain.order.OrderStatus;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class DefaultAdminOrderSearchService implements AdminOrderSearchUseCase {

    private final AdminOrderSearchPort searchPort;
    private final GuestPhoneProtector guestPhoneProtector;

    DefaultAdminOrderSearchService(AdminOrderSearchPort searchPort,
                                   GuestPhoneProtector guestPhoneProtector) {
        this.searchPort = searchPort;
        this.guestPhoneProtector = guestPhoneProtector;
    }

    @Override
    public OffsetPage<AdminOrderSearchRow> search(OrderStatus status, LocalDate dateFrom, LocalDate dateTo,
                                                   String keyword, int page, int size) {
        return AdminSearchHelper.search(
                searchPort, status, dateFrom, dateTo, keyword, page, size, this::toResponse);
    }

    private AdminOrderSearchRow toResponse(AdminOrderSearchResult result) {
        String buyerPhone = result.guestPhoneEnc() == null
                ? result.memberPhone()
                : guestPhoneProtector.decryptEncryptedPhone(result.guestPhoneEnc());
        return new AdminOrderSearchRow(
                result.orderId(),
                result.orderNumber(),
                result.status(),
                result.totalAmount(),
                result.buyerName(),
                buyerPhone,
                result.paidAt(),
                result.approvalDeadlineAt(),
                result.createdAt());
    }
}
