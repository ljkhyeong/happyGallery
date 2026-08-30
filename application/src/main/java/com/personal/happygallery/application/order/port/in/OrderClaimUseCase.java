package com.personal.happygallery.application.order.port.in;

import com.personal.happygallery.domain.order.OrderClaimResolution;
import com.personal.happygallery.domain.order.OrderClaimType;
import java.util.List;

public interface OrderClaimUseCase {

    OrderClaimView requestMemberClaim(Long orderId, Long userId, RequestCommand command);

    OrderClaimView requestGuestClaim(Long orderId, String accessToken, RequestCommand command);

    List<OrderClaimView> listMemberClaims(Long orderId, Long userId);

    List<OrderClaimView> listGuestClaims(Long orderId, String accessToken);

    record RequestCommand(
            OrderClaimType type,
            OrderClaimResolution requestedResolution,
            String reason,
            List<Item> items
    ) {}

    record Item(Long orderItemId, int quantity) {}
}
