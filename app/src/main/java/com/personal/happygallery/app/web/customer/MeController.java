package com.personal.happygallery.app.web.customer;

import com.personal.happygallery.app.order.OrderQueryService;
import com.personal.happygallery.app.order.OrderService;
import com.personal.happygallery.app.web.CustomerAuthFilter;
import com.personal.happygallery.app.web.order.dto.OrderDetailResponse;
import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderItem;
import com.personal.happygallery.domain.pass.PassPurchase;
import com.personal.happygallery.infra.booking.BookingRepository;
import com.personal.happygallery.infra.order.FulfillmentRepository;
import com.personal.happygallery.infra.order.OrderItemRepository;
import com.personal.happygallery.infra.order.OrderRepository;
import com.personal.happygallery.infra.pass.PassPurchaseRepository;
import com.personal.happygallery.infra.product.ProductRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원 전용 API — /api/v1/me 하위.
 * CustomerAuthFilter에 의해 인증이 보장된 상태에서만 도달한다.
 */
@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final FulfillmentRepository fulfillmentRepository;
    private final OrderService orderService;
    private final ProductRepository productRepository;
    private final BookingRepository bookingRepository;
    private final PassPurchaseRepository passRepository;

    public MeController(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        FulfillmentRepository fulfillmentRepository,
                        OrderService orderService,
                        ProductRepository productRepository,
                        BookingRepository bookingRepository,
                        PassPurchaseRepository passRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.fulfillmentRepository = fulfillmentRepository;
        this.orderService = orderService;
        this.productRepository = productRepository;
        this.bookingRepository = bookingRepository;
        this.passRepository = passRepository;
    }

    // ── 주문 ──

    @GetMapping("/orders")
    public List<MyOrderSummary> myOrders(HttpServletRequest request) {
        Long userId = getUserId(request);
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(MyOrderSummary::from)
                .toList();
    }

    @GetMapping("/orders/{id}")
    public OrderDetailResponse myOrder(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        Order order = orderRepository.findById(id)
                .filter(o -> Objects.equals(o.getUserId(), userId))
                .orElseThrow(() -> new NotFoundException("주문"));
        List<OrderItem> items = orderItemRepository.findByOrder(order);
        Fulfillment fulfillment = fulfillmentRepository.findByOrderId(id).orElse(null);
        return OrderDetailResponse.from(order, items, fulfillment);
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public MyOrderSummary createOrder(@RequestBody @Valid CreateMemberOrderRequest req,
                                      HttpServletRequest request) {
        Long userId = getUserId(request);
        List<OrderService.OrderItemRequest> items = req.items().stream()
                .map(i -> {
                    var product = productRepository.findById(i.productId())
                            .orElseThrow(() -> new NotFoundException("상품"));
                    return new OrderService.OrderItemRequest(i.productId(), i.qty(), product.getPrice());
                })
                .toList();
        Order order = orderService.createMemberOrder(userId, items);
        return MyOrderSummary.from(order);
    }

    // ── 예약 ──

    @GetMapping("/bookings")
    public List<MyBookingSummary> myBookings(HttpServletRequest request) {
        Long userId = getUserId(request);
        return bookingRepository.findByUserIdWithDetails(userId).stream()
                .map(MyBookingSummary::from)
                .toList();
    }

    @GetMapping("/bookings/{id}")
    public MyBookingDetail myBooking(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        Booking booking = bookingRepository.findById(id)
                .filter(b -> Objects.equals(b.getUserId(), userId))
                .orElseThrow(() -> new NotFoundException("예약"));
        return MyBookingDetail.from(booking);
    }

    // ── 8회권 ──

    @GetMapping("/passes")
    public List<MyPassSummary> myPasses(HttpServletRequest request) {
        Long userId = getUserId(request);
        return passRepository.findByUserIdOrderByPurchasedAtDesc(userId).stream()
                .map(MyPassSummary::from)
                .toList();
    }

    @GetMapping("/passes/{id}")
    public MyPassSummary myPass(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        PassPurchase pass = passRepository.findById(id)
                .filter(p -> Objects.equals(p.getUserId(), userId))
                .orElseThrow(() -> new NotFoundException("8회권"));
        return MyPassSummary.from(pass);
    }

    // ── 내부 ──

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute(CustomerAuthFilter.CUSTOMER_USER_ID_ATTR);
    }

    // ── DTO ──

    public record MyOrderSummary(Long orderId, String status, long totalAmount,
                                  LocalDateTime paidAt, LocalDateTime createdAt) {
        static MyOrderSummary from(Order o) {
            return new MyOrderSummary(o.getId(), o.getStatus().name(),
                    o.getTotalAmount(), o.getPaidAt(), o.getCreatedAt());
        }
    }

    public record MyBookingSummary(Long bookingId, String status, String className,
                                    LocalDateTime startAt, LocalDateTime endAt,
                                    long depositAmount) {
        static MyBookingSummary from(Booking b) {
            return new MyBookingSummary(b.getId(), b.getStatus().name(),
                    b.getBookingClass().getName(),
                    b.getSlot().getStartAt(), b.getSlot().getEndAt(),
                    b.getDepositAmount());
        }
    }

    public record MyBookingDetail(Long bookingId, String status, String className,
                                   LocalDateTime startAt, LocalDateTime endAt,
                                   long depositAmount, long balanceAmount,
                                   String balanceStatus, boolean passBooking) {
        static MyBookingDetail from(Booking b) {
            return new MyBookingDetail(b.getId(), b.getStatus().name(),
                    b.getBookingClass().getName(),
                    b.getSlot().getStartAt(), b.getSlot().getEndAt(),
                    b.getDepositAmount(), b.getBalanceAmount(),
                    b.getBalanceStatus().name(), b.isPassBooking());
        }
    }

    public record MyPassSummary(Long passId, LocalDateTime purchasedAt,
                                 LocalDateTime expiresAt, int totalCredits,
                                 int remainingCredits, long totalPrice) {
        static MyPassSummary from(PassPurchase p) {
            return new MyPassSummary(p.getId(), p.getPurchasedAt(), p.getExpiresAt(),
                    p.getTotalCredits(), p.getRemainingCredits(), p.getTotalPrice());
        }
    }

    public record CreateMemberOrderRequest(
            @NotEmpty List<OrderItemDto> items) {}

    public record OrderItemDto(
            @NotNull Long productId,
            @Min(1) int qty) {}
}
