package com.personal.happygallery.adapter.out.persistence.order;

import com.personal.happygallery.application.order.port.out.OrderClaimPort;
import com.personal.happygallery.application.order.port.out.OrderHistoryPort;
import com.personal.happygallery.application.order.port.out.OrderStorePort;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderApprovalHistory;
import com.personal.happygallery.domain.order.OrderClaim;
import com.personal.happygallery.domain.order.OrderClaimStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class JpaOrderPersistenceAdapter implements OrderStorePort,
        OrderHistoryPort,
        OrderClaimPort {

    private final OrderRepository orderRepository;
    private final OrderApprovalHistoryRepository orderHistoryRepository;
    private final OrderClaimRepository orderClaimRepository;

    JpaOrderPersistenceAdapter(
            OrderRepository orderRepository,
            OrderApprovalHistoryRepository orderHistoryRepository,
            OrderClaimRepository orderClaimRepository) {
        this.orderRepository = orderRepository;
        this.orderHistoryRepository = orderHistoryRepository;
        this.orderClaimRepository = orderClaimRepository;
    }

    @Override
    public Order save(Order order) {
        return orderRepository.save(order);
    }

    @Override
    public Order saveAndFlush(Order order) {
        return orderRepository.saveAndFlush(order);
    }

    @Override
    public OrderApprovalHistory save(OrderApprovalHistory history) {
        return orderHistoryRepository.save(history);
    }

    @Override
    public List<OrderApprovalHistory> findByOrderIdOrderByDecidedAtAsc(Long orderId) {
        return orderHistoryRepository.findByOrderIdOrderByDecidedAtAsc(orderId);
    }

    @Override
    public OrderClaim save(OrderClaim claim) {
        return orderClaimRepository.save(claim);
    }

    @Override
    public Optional<Long> findOrderIdById(Long id) {
        return orderClaimRepository.findOrderIdById(id);
    }

    @Override
    public Optional<OrderClaim> findByIdForUpdate(Long id) {
        return orderClaimRepository.findByIdForUpdate(id);
    }

    @Override
    public List<OrderClaim> findByOrderIdOrderByRequestedAtDesc(Long orderId) {
        return orderClaimRepository.findByOrderIdOrderByRequestedAtDesc(orderId);
    }

    @Override
    public List<OrderClaim> findRecent(int limit) {
        return orderClaimRepository.findRecent(limit);
    }

    @Override
    public List<OrderClaim> findRecentAfter(LocalDateTime requestedAt, Long id, int limit) {
        return orderClaimRepository.findRecentAfter(requestedAt, id, limit);
    }

    @Override
    public List<OrderClaim> findRecentByStatus(OrderClaimStatus status, int limit) {
        return orderClaimRepository.findRecentByStatus(status, limit);
    }

    @Override
    public List<OrderClaim> findRecentByStatusAfter(
            OrderClaimStatus status, LocalDateTime requestedAt, Long id, int limit) {
        return orderClaimRepository.findRecentByStatusAfter(status, requestedAt, id, limit);
    }

}
