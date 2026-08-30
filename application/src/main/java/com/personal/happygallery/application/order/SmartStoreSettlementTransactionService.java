package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.port.out.SmartStoreProductOrderPort;
import com.personal.happygallery.application.order.port.out.SmartStoreSettlementPort;
import com.personal.happygallery.application.order.port.out.SmartStoreSettlementProvider.SettlementItem;
import com.personal.happygallery.domain.order.SmartStoreProductOrder;
import com.personal.happygallery.domain.order.SmartStoreSettlementEntry;
import com.personal.happygallery.domain.order.SmartStoreSettlementStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class SmartStoreSettlementTransactionService {

    private final SmartStoreSettlementPort settlementPort;
    private final SmartStoreProductOrderPort orderPort;
    private final Clock clock;

    SmartStoreSettlementTransactionService(
            SmartStoreSettlementPort settlementPort,
            SmartStoreProductOrderPort orderPort,
            Clock clock) {
        this.settlementPort = settlementPort;
        this.orderPort = orderPort;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    SmartStoreSettlementStatus reconcile(SettlementItem item) {
        Reconciliation reconciliation = compare(item);
        String entryKey = entryKey(item);
        SmartStoreSettlementEntry entry = settlementPort.findByEntryKey(entryKey)
                .orElseGet(() -> SmartStoreSettlementEntry.create(entryKey));
        entry.synchronize(
                item.productOrderId(), item.orderId(), item.productOrderType(), item.settleType(),
                item.productName(), item.paySettleAmount(), item.totalPayCommissionAmount(),
                item.sellingInterlockCommissionAmount(), item.benefitSettleAmount(),
                item.settleExpectAmount(), item.settleBasisDate(), item.settleExpectDate(),
                item.settleCompleteDate(), item.payDate(), reconciliation.status(),
                reconciliation.reason(), LocalDateTime.now(clock));
        settlementPort.save(entry);
        return reconciliation.status();
    }

    private Reconciliation compare(SettlementItem item) {
        if (!"PROD_ORDER".equals(item.productOrderType())
                || !Set.of("NORMAL_SETTLE_ORIGINAL", "QUICK_SETTLE_ORIGINAL")
                        .contains(item.settleType())) {
            return Reconciliation.of(SmartStoreSettlementStatus.NOT_APPLICABLE, null);
        }
        SmartStoreProductOrder order = orderPort.findByProductOrderId(item.productOrderId())
                .orElse(null);
        if (order == null) {
            return Reconciliation.of(
                    SmartStoreSettlementStatus.ORDER_NOT_FOUND,
                    "같은 상품 주문 번호의 스마트스토어 주문 원장을 찾지 못했습니다.");
        }
        if (order.getExpectedSettlementAmount() == null) {
            return Reconciliation.of(
                    SmartStoreSettlementStatus.EXPECTED_AMOUNT_MISSING,
                    "주문 상세의 정산 예정 금액이 비어 있습니다.");
        }
        if (!Objects.equals(order.getExpectedSettlementAmount(), item.settleExpectAmount())) {
            return Reconciliation.of(
                    SmartStoreSettlementStatus.AMOUNT_MISMATCH,
                    "주문 상세의 정산 예정 금액과 실제 정산 원장의 예정 금액이 다릅니다.");
        }
        return Reconciliation.of(SmartStoreSettlementStatus.MATCHED, null);
    }

    private static String entryKey(SettlementItem item) {
        return String.join("|",
                value(item.productOrderId()), value(item.productOrderType()), value(item.settleType()),
                value(item.settleBasisDate()), value(item.settleExpectDate()),
                value(item.settleCompleteDate()), value(item.payDate()));
    }

    private static String value(Object value) {
        return Objects.toString(value, "");
    }

    private record Reconciliation(SmartStoreSettlementStatus status, String reason) {
        private static Reconciliation of(SmartStoreSettlementStatus status, String reason) {
            return new Reconciliation(status, reason);
        }
    }
}
