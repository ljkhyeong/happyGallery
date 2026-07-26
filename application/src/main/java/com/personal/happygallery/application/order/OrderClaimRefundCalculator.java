package com.personal.happygallery.application.order;

import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderClaimItem;
import com.personal.happygallery.domain.order.OrderItem;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class OrderClaimRefundCalculator {

    private OrderClaimRefundCalculator() {}

    static long maximumRefundAmount(
            Order order, List<OrderClaimLine> lines, List<OrderItem> allOrderItems) {
        long itemAmount = lines.stream()
                .mapToLong(OrderClaimLine::amount)
                .sum();
        Map<Long, Integer> claimedQuantityByItemId = new HashMap<>();
        lines.forEach(line -> claimedQuantityByItemId.put(
                line.orderItem().getId(), line.claimItem().getQuantity()));
        boolean fullOrderClaim = allOrderItems.stream()
                .allMatch(item -> claimedQuantityByItemId.getOrDefault(item.getId(), 0) == item.getQty());
        return Math.addExact(itemAmount, fullOrderClaim ? order.getShippingFee() : 0L);
    }

    static void allocateRefundAmount(List<OrderClaimLine> lines, long refundAmount) {
        long productAmount = lines.stream()
                .mapToLong(OrderClaimLine::amount)
                .reduce(0L, Math::addExact);
        long allocatableAmount = Math.min(refundAmount, productAmount);
        BigInteger productTotal = BigInteger.valueOf(productAmount);
        BigInteger allocationTotal = BigInteger.valueOf(allocatableAmount);

        List<RefundAllocation> allocations = lines.stream()
                .map(line -> {
                    BigInteger[] result = allocationTotal
                            .multiply(BigInteger.valueOf(line.amount()))
                            .divideAndRemainder(productTotal);
                    return new RefundAllocation(
                            line.claimItem(), result[0].longValueExact(), result[1]);
                })
                .toList();
        long unallocatedWon = allocatableAmount
                - allocations.stream().mapToLong(RefundAllocation::amount).sum();
        List<RefundAllocation> byLargestRemainder = new ArrayList<>(allocations);
        byLargestRemainder.sort(Comparator
                .comparing(RefundAllocation::remainder, Comparator.reverseOrder())
                .thenComparing(allocation -> allocation.item().getOrderItemId()));

        for (int index = 0; index < byLargestRemainder.size(); index++) {
            RefundAllocation allocation = byLargestRemainder.get(index);
            allocation.item().allocateApprovedRefundAmount(
                    allocation.amount() + (index < unallocatedWon ? 1L : 0L));
        }
    }

    private record RefundAllocation(
            OrderClaimItem item, long amount, BigInteger remainder) {}
}
