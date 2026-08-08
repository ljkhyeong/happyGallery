package com.personal.happygallery.application.order;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderItem;
import com.personal.happygallery.domain.order.ProportionalAmountAllocator;
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
        Map<Long, Integer> claimedQuantityByItemId = new HashMap<>();
        lines.forEach(line -> claimedQuantityByItemId.put(
                line.orderItem().getId(), line.claimItem().getQuantity()));
        boolean fullOrderClaim = allOrderItems.stream()
                .allMatch(item -> claimedQuantityByItemId.getOrDefault(item.getId(), 0) == item.getQty());
        return maximumRefundAmount(order, lines, fullOrderClaim);
    }

    static long maximumRefundAmount(
            Order order, List<OrderClaimLine> lines, boolean fullOrderClaim) {
        long productCustomerAmount = lines.stream()
                .mapToLong(OrderClaimLine::customerRefundableAmount)
                .reduce(0L, Math::addExact);
        return Math.addExact(productCustomerAmount, fullOrderClaim ? order.getShippingFee() : 0L);
    }

    static RefundAllocation allocateRefundAmount(
            Order order,
            List<OrderClaimLine> lines,
            long customerRefundAmount,
            long previousProductPgRefundAmount,
            long earnedRewardAmount,
            long reservedRewardRevokeAmount) {
        List<OrderClaimLine> orderedLines = lines.stream()
                .sorted(Comparator.comparing(line -> line.orderItem().getId()))
                .toList();
        long productCustomerMaximum = orderedLines.stream()
                .mapToLong(OrderClaimLine::customerRefundableAmount)
                .reduce(0L, Math::addExact);
        long productCustomerRefundAmount = Math.min(customerRefundAmount, productCustomerMaximum);
        long shippingRefundAmount = customerRefundAmount - productCustomerRefundAmount;

        List<Long> componentBases = new ArrayList<>(orderedLines.size() * 2);
        for (OrderClaimLine line : orderedLines) {
            componentBases.add(line.pgRefundableAmount());
            componentBases.add(line.rewardRestorableAmount());
        }
        List<Long> componentAllocations = ProportionalAmountAllocator.allocate(
                productCustomerRefundAmount, componentBases);

        long productPgRefundAmount = 0L;
        long rewardRestoreAmount = 0L;
        for (int index = 0; index < orderedLines.size(); index++) {
            long pgAmount = componentAllocations.get(index * 2);
            long rewardAmount = componentAllocations.get(index * 2 + 1);
            orderedLines.get(index).claimItem().allocateApprovedRefund(
                    Math.addExact(pgAmount, rewardAmount), rewardAmount);
            productPgRefundAmount = Math.addExact(productPgRefundAmount, pgAmount);
            rewardRestoreAmount = Math.addExact(rewardRestoreAmount, rewardAmount);
        }
        long pgRefundAmount = Math.addExact(productPgRefundAmount, shippingRefundAmount);
        if (Math.addExact(pgRefundAmount, rewardRestoreAmount) != customerRefundAmount) {
            throw invalid("환불 수단별 배분 합계가 고객 반환액과 일치하지 않습니다.");
        }
        long rewardRevokeAmount = calculateRewardRevokeAmount(
                order,
                previousProductPgRefundAmount,
                productPgRefundAmount,
                earnedRewardAmount,
                reservedRewardRevokeAmount);
        return new RefundAllocation(
                pgRefundAmount,
                customerRefundAmount,
                rewardRestoreAmount,
                rewardRevokeAmount,
                productPgRefundAmount,
                shippingRefundAmount);
    }

    private static long calculateRewardRevokeAmount(
            Order order,
            long previousProductPgRefundAmount,
            long productPgRefundAmount,
            long earnedRewardAmount,
            long reservedRewardRevokeAmount) {
        if (previousProductPgRefundAmount < 0L
                || productPgRefundAmount < 0L
                || earnedRewardAmount < 0L
                || reservedRewardRevokeAmount < 0L
                || reservedRewardRevokeAmount > earnedRewardAmount) {
            throw invalid("환불 혜택 누계가 올바르지 않습니다.");
        }
        long cumulativeProductPgRefund = Math.addExact(
                previousProductPgRefundAmount, productPgRefundAmount);
        if (cumulativeProductPgRefund > order.getRewardEarnBase()) {
            throw invalid("상품 PG 환불 누계가 주문의 적립 기준액을 초과합니다.");
        }
        if (earnedRewardAmount == 0L || order.getRewardEarnBase() == 0L) {
            return 0L;
        }
        long cumulativeTarget = BigInteger.valueOf(earnedRewardAmount)
                .multiply(BigInteger.valueOf(cumulativeProductPgRefund))
                .divide(BigInteger.valueOf(order.getRewardEarnBase()))
                .longValueExact();
        return Math.max(0L, cumulativeTarget - reservedRewardRevokeAmount);
    }

    private static HappyGalleryException invalid(String message) {
        return new HappyGalleryException(ErrorCode.CONFLICT, message);
    }

    record RefundAllocation(
            long pgRefundAmount,
            long customerRefundAmount,
            long rewardRestoreAmount,
            long rewardRevokeAmount,
            long productPgRefundAmount,
            long shippingRefundAmount) {}
}
