package com.personal.happygallery.app.order;

import com.personal.happygallery.common.error.PhoneVerificationFailedException;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.booking.PhoneVerification;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.infra.booking.GuestRepository;
import com.personal.happygallery.infra.booking.PhoneVerificationRepository;
import com.personal.happygallery.infra.product.ProductRepository;
import com.personal.happygallery.common.error.NotFoundException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공개 주문 생성 — 휴대폰 인증 기반.
 */
@Service
@Transactional
public class OrderCreationService {

    private final PhoneVerificationRepository phoneVerificationRepository;
    private final GuestRepository guestRepository;
    private final ProductRepository productRepository;
    private final OrderService orderService;
    private final Clock clock;

    public OrderCreationService(PhoneVerificationRepository phoneVerificationRepository,
                                GuestRepository guestRepository,
                                ProductRepository productRepository,
                                OrderService orderService,
                                Clock clock) {
        this.phoneVerificationRepository = phoneVerificationRepository;
        this.guestRepository = guestRepository;
        this.productRepository = productRepository;
        this.orderService = orderService;
        this.clock = clock;
    }

    public record OrderItemInput(Long productId, int qty) {}

    /**
     * 휴대폰 인증 기반 주문 생성.
     */
    public Order createOrderByPhone(String phone, String verificationCode,
                                     String name, List<OrderItemInput> items) {
        // 인증 코드 검증
        PhoneVerification pv = phoneVerificationRepository
                .findByPhoneAndCodeAndVerifiedFalseAndExpiresAtAfter(
                        phone, verificationCode, LocalDateTime.now(clock))
                .orElseThrow(PhoneVerificationFailedException::new);
        pv.markVerified();

        // Guest upsert
        Guest guest = guestRepository.findByPhone(phone)
                .orElseGet(() -> guestRepository.save(new Guest(name, phone)));
        guest.markPhoneVerified();

        // 상품 가격 조회 후 OrderItemRequest 변환
        List<OrderService.OrderItemRequest> orderItems = items.stream()
                .map(item -> {
                    Product product = productRepository.findById(item.productId())
                            .orElseThrow(() -> new NotFoundException("상품"));
                    return new OrderService.OrderItemRequest(
                            item.productId(), item.qty(), product.getPrice());
                })
                .toList();

        return orderService.createPaidOrder(guest.getId(), orderItems);
    }
}
