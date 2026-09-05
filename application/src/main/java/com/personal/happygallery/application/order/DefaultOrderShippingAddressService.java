package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.port.in.OrderShippingAddressUseCase;
import com.personal.happygallery.application.order.port.out.FulfillmentPort;
import com.personal.happygallery.application.order.port.out.OrderReaderPort;
import com.personal.happygallery.application.order.port.out.ShippingAddressChangePort;
import com.personal.happygallery.application.token.GuestTokenService;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.ShippingAddress;
import com.personal.happygallery.domain.order.ShippingAddressChange;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultOrderShippingAddressService implements OrderShippingAddressUseCase {
    private final OrderReaderPort orders;
    private final FulfillmentPort fulfillments;
    private final ShippingAddressChangePort changes;
    private final ShippingAddressProtector protector;
    private final GuestTokenService tokens;
    private final Clock clock;

    public DefaultOrderShippingAddressService(OrderReaderPort orders, FulfillmentPort fulfillments,
            ShippingAddressChangePort changes, ShippingAddressProtector protector,
            GuestTokenService tokens, Clock clock) {
        this.orders = orders;
        this.fulfillments = fulfillments;
        this.changes = changes;
        this.protector = protector;
        this.tokens = tokens;
        this.clock = clock;
    }

    @Override
    public void updateMember(Long orderId, Long userId, long version, ShippingAddress address) {
        Order order = orders.findByIdForUpdate(orderId)
                .filter(value -> Objects.equals(value.getUserId(), userId))
                .orElseThrow(NotFoundException.supplier("주문"));
        update(order, version, address);
    }

    @Override
    public void updateGuest(Long orderId, String accessToken, long version, ShippingAddress address) {
        String hash = tokens.resolveTokenHash(accessToken);
        Order order = orders.findByIdForUpdate(orderId)
                .filter(value -> value.getGuestId() != null && Objects.equals(value.getAccessToken(), hash))
                .orElseThrow(NotFoundException.supplier("주문"));
        update(order, version, address);
    }

    private void update(Order order, long version, ShippingAddress address) {
        order.getStatus().requireShippingAddressWritable();
        var fulfillment = OrderLookups.requireFulfillment(fulfillments, order.getId());
        String before = fulfillment.getShippingAddressEnc();
        String after = protector.encrypt(address);
        fulfillment.changeShippingAddress(after, version);
        changes.save(new ShippingAddressChange(order, before, after, LocalDateTime.now(clock)));
        fulfillments.save(fulfillment);
    }
}
