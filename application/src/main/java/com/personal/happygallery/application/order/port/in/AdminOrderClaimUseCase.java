package com.personal.happygallery.application.order.port.in;

import com.personal.happygallery.domain.order.OrderClaimStatus;
import java.util.List;

public interface AdminOrderClaimUseCase {

    List<OrderClaimView> list(OrderClaimStatus status, int size);

    OrderClaimView resolve(Long claimId, Long adminId, ResolveCommand command);

    OrderClaimView completeExchange(Long claimId, Long adminId, CompleteExchangeCommand command);

    record ResolveCommand(
            boolean approved,
            Long refundAmount,
            boolean restoreInventory,
            String note
    ) {}

    record CompleteExchangeCommand(
            String carrier,
            String trackingNumber,
            String note
    ) {}
}
