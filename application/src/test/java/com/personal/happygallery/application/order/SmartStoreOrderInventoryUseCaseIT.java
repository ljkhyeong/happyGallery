package com.personal.happygallery.application.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.personal.happygallery.adapter.out.persistence.order.SmartStoreProductOrderRepository;
import com.personal.happygallery.adapter.out.persistence.order.SmartStoreOrderActionHistoryRepository;
import com.personal.happygallery.adapter.out.persistence.product.InventoryRepository;
import com.personal.happygallery.adapter.out.persistence.product.ProductVariantRepository;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.CompletedReturn;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.ProductOrderChange;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.ProductOrderDetail;
import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase;
import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase.AdminActor;
import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase.InventoryResolutionCommand;
import com.personal.happygallery.application.product.InventoryService;
import com.personal.happygallery.application.product.ProductVariantStockService;
import com.personal.happygallery.application.product.ProductVariantStockService.VariantAdjustment;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase.SaveProductCommand;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.SaveMappingCommand;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.VariantMapping;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.order.SmartStoreInventoryResolutionAction;
import com.personal.happygallery.domain.order.SmartStoreOrderAction;
import com.personal.happygallery.domain.order.SmartStoreOrderActionHistory;
import com.personal.happygallery.domain.order.SmartStoreOrderAttentionReason;
import com.personal.happygallery.domain.order.SmartStoreOrderReconciliationOutcome;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.LocalDateTime;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@UseCaseIT
class SmartStoreOrderInventoryUseCaseIT {

    private static final LocalDateTime CHANGED_AT = LocalDateTime.of(2026, 8, 31, 12, 0);

    @Autowired ProductAdminUseCase productAdminUseCase;
    @Autowired SmartStoreInventoryUseCase inventoryUseCase;
    @Autowired SmartStoreOrderTransactionService orderService;
    @Autowired SmartStoreChannelOrderUseCase channelOrderUseCase;
    @Autowired SmartStoreProductOrderRepository orderRepository;
    @Autowired SmartStoreOrderActionHistoryRepository actionHistoryRepository;
    @Autowired InventoryRepository inventoryRepository;
    @Autowired ProductVariantRepository variantRepository;
    @Autowired InventoryService inventoryService;
    @Autowired ProductVariantStockService variantStockService;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired JdbcTemplate jdbc;
    @Autowired Clock clock;

    @AfterEach
    void tearDown() {
        orderRepository.deleteAllInBatch();
        cleanupSupport.clearProductData();
    }

    @ParameterizedTest
    @EnumSource(ProductType.class)
    @DisplayName("재고 부족 주문을 저장하고 다음 주문을 처리하며 입고 후 재시도는 한 번만 차감한다")
    void shortage_isPersistedAndRetriedWithoutBlockingOtherOrders(ProductType type) {
        boolean madeToOrder = type == ProductType.MADE_TO_ORDER;
        var registered = productAdminUseCase.register(new SaveProductCommand(
                "채널 주문 재고", type, null, 35000L, 1, null, null,
                madeToOrder ? "가죽 제품" : null, null, madeToOrder ? 7 : null,
                List.of(), List.of()));
        Long productId = registered.product().getId();
        Long variantId = madeToOrder ? registered.options().variants().getFirst().id() : null;
        Long optionId = madeToOrder ? 11L : null;
        inventoryUseCase.saveMapping(productId, new SaveMappingCommand(123L, true,
                madeToOrder ? List.of(new VariantMapping(variantId, optionId)) : List.of()));

        orderService.synchronize(detail("shortage", optionId, 2), change("shortage"));

        var shortage = orderRepository.findById("shortage").orElseThrow();
        assertThat(shortage.getAttentionReason()).isEqualTo(SmartStoreOrderAttentionReason.STOCK_SHORTAGE);
        assertThat(shortage.getInventoryAppliedQuantity()).isZero();
        assertThat(quantity(productId, variantId)).isEqualTo(1);

        orderService.synchronize(detail("next-order", optionId, 1), change("next-order"));
        assertThat(orderRepository.findById("next-order").orElseThrow().getInventoryAppliedQuantity()).isEqualTo(1);
        assertThat(quantity(productId, variantId)).isZero();

        if (madeToOrder) {
            variantStockService.restoreAll(List.of(new VariantAdjustment(variantId, 2)));
        } else {
            inventoryService.restore(productId, 2);
        }
        orderService.retryInventory("shortage");
        orderService.retryInventory("shortage");

        var retried = orderRepository.findById("shortage").orElseThrow();
        assertThat(retried.getAttentionReason()).isNull();
        assertThat(retried.getInventoryAppliedQuantity()).isEqualTo(2);
        assertThat(quantity(productId, variantId)).isZero();
    }

    @Test
    @DisplayName("관리자는 매핑 누락 주문을 상품에 연결하고 남은 수량을 재고와 처리 이력에 함께 반영한다")
    void resolveInventory_mappingRequired_appliesInventoryAndAuditTogether() {
        var registered = productAdminUseCase.register(new SaveProductCommand(
                "수동 연결 상품", ProductType.READY_STOCK, null, 35000L, 3, null, null,
                null, null, null, List.of(), List.of()));
        Long productId = registered.product().getId();
        orderService.synchronize(detail("manual-resolution", null, 2), change("manual-resolution"));
        var pending = orderRepository.findById("manual-resolution").orElseThrow();

        var resolved = channelOrderUseCase.resolveInventory(new InventoryResolutionCommand(
                "manual-resolution",
                productId,
                null,
                SmartStoreInventoryResolutionAction.APPLY_REMAINING,
                "기존 스마트스토어 상품을 내부 상품에 연결",
                pending.inventoryResolutionVersion()), new AdminActor(17L, "운영 관리자"));

        assertThat(resolved.productId()).isEqualTo(productId);
        assertThat(resolved.inventoryAppliedQuantity()).isEqualTo(2);
        assertThat(resolved.attentionReason()).isNull();
        assertThat(quantity(productId, null)).isEqualTo(1);
        assertThat(jdbc.queryForMap("""
                SELECT action, status, changed_by_admin_id, changed_by, request_summary
                FROM smartstore_order_action_history
                WHERE product_order_id = 'manual-resolution'
                """))
                .containsEntry("action", "INVENTORY_RESOLVED")
                .containsEntry("status", "SUCCEEDED")
                .containsEntry("changed_by_admin_id", 17L)
                .containsEntry("changed_by", "운영 관리자");
    }

    @Test
    @DisplayName("관리자가 오래된 재고 확인 버전으로 수동 결정을 제출하면 현재 주문을 변경하지 않는다")
    void resolveInventory_staleVersion_rejectsWithoutChangingInventory() {
        var registered = productAdminUseCase.register(new SaveProductCommand(
                "수동 연결 충돌", ProductType.READY_STOCK, null, 35000L, 3, null, null,
                null, null, null, List.of(), List.of()));
        Long productId = registered.product().getId();
        orderService.synchronize(detail("stale-resolution", null, 2), change("stale-resolution"));

        assertThatThrownBy(() -> channelOrderUseCase.resolveInventory(new InventoryResolutionCommand(
                "stale-resolution",
                productId,
                null,
                SmartStoreInventoryResolutionAction.APPLY_REMAINING,
                "오래된 화면에서 처리",
                "stale-version"), new AdminActor(17L, "운영 관리자")))
                .isInstanceOfSatisfying(HappyGalleryException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));

        assertThat(quantity(productId, null)).isEqualTo(3);
        assertThat(orderRepository.findById("stale-resolution").orElseThrow().getProductId()).isNull();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM smartstore_order_action_history WHERE product_order_id = ?",
                Integer.class,
                "stale-resolution")).isZero();
    }

    @Test
    @DisplayName("확인 대상 주문 목록은 사유 필터와 커서로 중복 없이 다음 페이지를 조회한다")
    void listAttentionOrders_filtersReasonAndUsesCursor() {
        orderService.synchronize(detail("cursor-1", null, 1),
                new ProductOrderChange("cursor-1", "PAYED", CHANGED_AT.plusMinutes(1)));
        orderService.synchronize(detail("cursor-2", null, 1),
                new ProductOrderChange("cursor-2", "PAYED", CHANGED_AT.plusMinutes(2)));

        var first = channelOrderUseCase.list(
                true, SmartStoreOrderAttentionReason.MAPPING_REQUIRED, null, 1);
        var second = channelOrderUseCase.list(
                true, SmartStoreOrderAttentionReason.MAPPING_REQUIRED, first.nextCursor(), 1);

        assertThat(first.content()).extracting(SmartStoreChannelOrderUseCase.ChannelOrderResult::productOrderId)
                .containsExactly("cursor-2");
        assertThat(first.hasMore()).isTrue();
        assertThat(second.content()).extracting(SmartStoreChannelOrderUseCase.ChannelOrderResult::productOrderId)
                .containsExactly("cursor-1");
        assertThat(second.hasMore()).isFalse();
    }

    @Test
    @DisplayName("결과 미확정과 장기 요청만 대사 작업함에 표시하고 관리자 확인 뒤 제거한다")
    void unresolvedActions_arePagedAndReconciledWithAdminEvidence() {
        LocalDateTime now = LocalDateTime.now(clock);
        SmartStoreOrderActionHistory unknown = new SmartStoreOrderActionHistory(
                "unknown-action", SmartStoreOrderAction.ORDER_CONFIRMED, null,
                7L, "주문 관리자", now.minusMinutes(1));
        unknown.markResultUnknown("응답 대기 중 연결 종료", now.minusMinutes(1));
        SmartStoreOrderActionHistory stale = new SmartStoreOrderActionHistory(
                "stale-action", SmartStoreOrderAction.ORDER_DISPATCHED, "운송장 1234",
                7L, "주문 관리자", now.minusMinutes(10));
        SmartStoreOrderActionHistory notSent = new SmartStoreOrderActionHistory(
                "not-sent-action", SmartStoreOrderAction.ORDER_CONFIRMED, null,
                7L, "주문 관리자", now.minusMinutes(20));
        notSent.markNotSent("ACCESS_TOKEN_UNAVAILABLE", "인증 토큰 준비 실패", now.minusMinutes(20));
        actionHistoryRepository.saveAllAndFlush(List.of(unknown, stale, notSent));

        var first = channelOrderUseCase.listUnresolvedActions(null, 1);
        var second = channelOrderUseCase.listUnresolvedActions(first.nextCursor(), 1);

        assertThat(first.content()).extracting(SmartStoreChannelOrderUseCase.ActionHistoryResult::productOrderId)
                .containsExactly("unknown-action");
        assertThat(first.hasMore()).isTrue();
        assertThat(second.content()).extracting(SmartStoreChannelOrderUseCase.ActionHistoryResult::productOrderId)
                .containsExactly("stale-action");
        assertThat(second.hasMore()).isFalse();

        var reconciled = channelOrderUseCase.reconcileAction(
                unknown.getId(),
                new SmartStoreChannelOrderUseCase.ReconcileActionCommand(
                        SmartStoreOrderReconciliationOutcome.APPLIED,
                        "네이버 주문 상세에서 발주 완료 상태를 확인"),
                new AdminActor(19L, "대사 관리자"));

        assertThat(reconciled.reconciliationOutcome())
                .isEqualTo(SmartStoreOrderReconciliationOutcome.APPLIED);
        assertThat(reconciled.reconciledByAdminId()).isEqualTo(19L);
        assertThat(channelOrderUseCase.listUnresolvedActions(null, 10).content())
                .extracting(SmartStoreChannelOrderUseCase.ActionHistoryResult::productOrderId)
                .containsExactly("stale-action");
        assertThatThrownBy(() -> channelOrderUseCase.reconcileAction(
                unknown.getId(),
                new SmartStoreChannelOrderUseCase.ReconcileActionCommand(
                        SmartStoreOrderReconciliationOutcome.NOT_APPLIED, "중복 대사"),
                new AdminActor(19L, "대사 관리자")))
                .isInstanceOfSatisfying(HappyGalleryException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
    }

    @Test
    @DisplayName("판매 뒤 중지된 주문제작 옵션 조합도 늦게 수집된 스마트스토어 주문 재고를 차감한다")
    void synchronize_inactiveVariant_deductsCommittedChannelSale() {
        var registered = productAdminUseCase.register(new SaveProductCommand(
                "판매 중지 옵션 주문", ProductType.MADE_TO_ORDER, null, 35000L, 2, null, null,
                "가죽 제품", null, 7, List.of(), List.of()));
        Long productId = registered.product().getId();
        Long variantId = registered.options().variants().getFirst().id();
        inventoryUseCase.saveMapping(productId, new SaveMappingCommand(
                123L, true, List.of(new VariantMapping(variantId, 11L))));
        var variant = variantRepository.findById(variantId).orElseThrow();
        variant.deactivate();
        variantRepository.saveAndFlush(variant);

        orderService.synchronize(detail("inactive-variant", 11L, 1), change("inactive-variant"));

        var order = orderRepository.findById("inactive-variant").orElseThrow();
        assertThat(order.getInventoryAppliedQuantity()).isEqualTo(1);
        assertThat(order.getAttentionReason()).isNull();
        assertThat(quantity(productId, variantId)).isEqualTo(1);
    }

    private int quantity(Long productId, Long variantId) {
        return variantId == null
                ? inventoryRepository.findById(productId).orElseThrow().getQuantity()
                : variantRepository.findById(variantId).orElseThrow().getQuantity();
    }

    @ParameterizedTest
    @CsvSource({"READY_STOCK, true", "READY_STOCK, false", "MADE_TO_ORDER, true", "MADE_TO_ORDER, false"})
    @DisplayName("배송 중 부분반품의 검수 결과는 재수집과 취소 후에도 유지하고 추가 반품만 새로 검수한다")
    void resolvedReturn_survivesRetryAndRefresh_andReviewsOnlyAdditionalQuantity(
            ProductType type, boolean restoreStock) {
        boolean madeToOrder = type == ProductType.MADE_TO_ORDER;
        var registered = productAdminUseCase.register(new SaveProductCommand(
                "반품 검수 재고", type, null, 35000L, 5, null, null,
                madeToOrder ? "가죽 제품" : null, null, madeToOrder ? 7 : null,
                List.of(), List.of()));
        Long productId = registered.product().getId();
        Long variantId = madeToOrder ? registered.options().variants().getFirst().id() : null;
        Long optionId = madeToOrder ? 11L : null;
        inventoryUseCase.saveMapping(productId, new SaveMappingCommand(123L, true,
                madeToOrder ? List.of(new VariantMapping(variantId, optionId)) : List.of()));
        orderService.synchronize(detail("returned", optionId, 5), change("returned"));
        var firstReturns = List.of(new CompletedReturn("return-1", 2, CHANGED_AT.plusMinutes(1)));
        ProductOrderDetail returned = claimDetail(optionId, "DELIVERING", "RETURN", 3, firstReturns);
        ProductOrderChange returnChange = new ProductOrderChange(
                "returned", "CLAIM_COMPLETED", CHANGED_AT.plusMinutes(1));
        orderService.synchronize(returned, returnChange);

        assertThat(quantity(productId, variantId)).isZero();
        assertThat(orderRepository.findById("returned").orElseThrow().getAttentionReason())
                .isEqualTo(SmartStoreOrderAttentionReason.RETURN_REVIEW);

        String reviewVersion = orderRepository.findById("returned").orElseThrow().returnReviewVersion();
        orderService.resolveReturn("returned", restoreStock, reviewVersion);
        orderService.retryInventory("returned");
        orderService.synchronize(returned, returnChange);
        orderService.synchronize(returned, new ProductOrderChange(
                "returned", "CLAIM_COMPLETED", CHANGED_AT.plusMinutes(2)));

        assertThat(orderRepository.findById("returned").orElseThrow().getAttentionReason()).isNull();
        assertThat(quantity(productId, variantId)).isEqualTo(restoreStock ? 2 : 0);
        assertThatThrownBy(() -> orderService.resolveReturn("returned", !restoreStock, reviewVersion))
                .isInstanceOf(HappyGalleryException.class);

        orderService.synchronize(claimDetail(optionId, "DELIVERED", "CANCEL", 2, firstReturns),
                new ProductOrderChange("returned", "CLAIM_COMPLETED", CHANGED_AT.plusMinutes(3)));
        assertThat(quantity(productId, variantId)).isEqualTo(restoreStock ? 3 : 1);
        assertThat(orderRepository.findById("returned").orElseThrow().getAttentionReason()).isNull();

        var allReturns = List.of(firstReturns.getFirst(),
                new CompletedReturn("return-2", 2, CHANGED_AT.plusMinutes(4)));
        orderService.synchronize(claimDetail(optionId, "RETURNED", "RETURN", 0, allReturns), new ProductOrderChange(
                "returned", "CLAIM_COMPLETED", CHANGED_AT.plusMinutes(4)));
        assertThat(orderRepository.findById("returned").orElseThrow().getAttentionReason())
                .isEqualTo(SmartStoreOrderAttentionReason.RETURN_REVIEW);
        orderService.resolveReturn("returned", true,
                orderRepository.findById("returned").orElseThrow().returnReviewVersion());
        orderService.retryInventory("returned");

        var resolved = orderRepository.findById("returned").orElseThrow();
        assertThat(resolved.getAttentionReason()).isNull();
        assertThat(resolved.getInventoryAppliedQuantity()).isEqualTo(restoreStock ? 0 : 2);
        assertThat(quantity(productId, variantId)).isEqualTo(restoreStock ? 5 : 3);
    }

    private static ProductOrderDetail claimDetail(
            Long optionId, String status, String claimType, int remainQuantity, List<CompletedReturn> returns) {
        return new ProductOrderDetail(
                "returned", "order-returned", 123L, optionId, "반품 검수 재고", null, null, status,
                null, claimType, claimType + "_DONE", null, 5, remainQuantity, CHANGED_AT.minusMinutes(1), null,
                null, null, null, null, null, null, null, null, null, returns);
    }

    @ParameterizedTest
    @CsvSource({"false, true", "false, false", "true, true", "true, false"})
    @DisplayName("추가 반품이나 다른 관리자 검수 뒤에는 수량이 같아도 이전 확인값으로 복원·종료하지 못한다")
    void changedReturnReview_rejectsPreviousSnapshot(boolean previousReviewed, boolean restoreStock) {
        var registered = productAdminUseCase.register(new SaveProductCommand(
                "검수 대상 변경", ProductType.READY_STOCK, null, 35000L, 5, null, null,
                null, null, null, List.of(), List.of()));
        Long productId = registered.product().getId();
        inventoryUseCase.saveMapping(productId, new SaveMappingCommand(123L, true, List.of()));
        orderService.synchronize(detail("returned", null, 5), change("returned"));
        var first = new CompletedReturn("return-1", 2, CHANGED_AT.plusMinutes(1));
        orderService.synchronize(claimDetail(null, "DELIVERING", "RETURN", 3, List.of(first)),
                new ProductOrderChange("returned", "CLAIM_COMPLETED", CHANGED_AT.plusMinutes(1)));
        String previousVersion = orderRepository.findById("returned").orElseThrow().returnReviewVersion();
        if (previousReviewed) {
            orderService.resolveReturn("returned", false, previousVersion);
        }
        var second = new CompletedReturn("return-2", 2, CHANGED_AT.plusMinutes(2));
        orderService.synchronize(claimDetail(null, "DELIVERING", "RETURN", 1, List.of(first, second)),
                new ProductOrderChange("returned", "CLAIM_COMPLETED", CHANGED_AT.plusMinutes(2)));

        assertThatThrownBy(() -> orderService.resolveReturn("returned", restoreStock, previousVersion))
                .isInstanceOfSatisfying(HappyGalleryException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        var current = orderRepository.findById("returned").orElseThrow();
        assertThat(current.pendingReturnQuantity()).isEqualTo(previousReviewed ? 2 : 4);
        assertThat(current.getAttentionReason()).isEqualTo(SmartStoreOrderAttentionReason.RETURN_REVIEW);
        assertThat(quantity(productId, null)).isZero();

        orderService.resolveReturn("returned", restoreStock, current.returnReviewVersion());
        assertThat(quantity(productId, null)).isEqualTo(restoreStock ? current.pendingReturnQuantity() : 0);
        assertThat(orderRepository.findById("returned").orElseThrow().pendingReturnQuantity()).isZero();
    }

    @ParameterizedTest
    @CsvSource({"false, true", "false, false", "true, false"})
    @DisplayName("기존 반품 주문의 검수 대기와 복원 여부는 완료 이력으로 전환한 뒤에도 유지한다")
    void legacyReturn_initializationPreservesInventoryAndReview(boolean restored, boolean pendingReview) {
        var registered = productAdminUseCase.register(new SaveProductCommand(
                "기존 반품", ProductType.READY_STOCK, null, 35000L, 5, null, null,
                null, null, null, List.of(), List.of()));
        Long productId = registered.product().getId();
        inventoryUseCase.saveMapping(productId, new SaveMappingCommand(123L, true, List.of()));
        orderService.synchronize(detail("returned", null, 5), change("returned"));
        orderService.synchronize(claimDetail(null, "PAYED", "CANCEL", 2, List.of()),
                new ProductOrderChange("returned", "CLAIM_COMPLETED", CHANGED_AT.plusMinutes(1)));
        if (restored) {
            inventoryService.restore(productId, 2);
        }
        jdbc.update("""
                UPDATE smartstore_product_orders
                SET product_order_status = 'RETURNED', claim_type = 'RETURN', claim_status = 'RETURN_DONE',
                    remain_quantity = 0, inventory_applied_quantity = ?, attention_reason = ?,
                    return_reviewed_remain_quantity = ?, completed_return_quantity = NULL,
                    last_changed_at = ?
                WHERE product_order_id = 'returned'
                """, restored ? 0 : 2, pendingReview ? "RETURN_REVIEW" : null,
                pendingReview ? null : 0, CHANGED_AT.plusMinutes(2));

        var returned = claimDetail(null, "RETURNED", "RETURN", 0,
                List.of(new CompletedReturn("legacy-return", 2, CHANGED_AT.plusMinutes(2))));
        orderService.synchronize(returned,
                new ProductOrderChange("returned", "CLAIM_COMPLETED", CHANGED_AT.plusMinutes(3)));
        orderService.retryInventory("returned");

        assertThat(quantity(productId, null)).isEqualTo(restored ? 5 : 3);
        assertThat(orderRepository.findById("returned").orElseThrow().getAttentionReason())
                .isEqualTo(pendingReview ? SmartStoreOrderAttentionReason.RETURN_REVIEW : null);
        assertThat(jdbc.queryForMap("""
                SELECT completed_return_quantity, reviewed_return_quantity, restored_return_quantity,
                       return_reviewed_remain_quantity
                FROM smartstore_product_orders WHERE product_order_id = 'returned'
                """))
                .containsEntry("completed_return_quantity", 2)
                .containsEntry("reviewed_return_quantity", pendingReview ? 0 : 2)
                .containsEntry("restored_return_quantity", restored ? 2 : 0)
                .containsEntry("return_reviewed_remain_quantity", null);
    }

    private static ProductOrderDetail detail(String id, Long optionId, int quantity) {
        return new ProductOrderDetail(
                id, "order-" + id, 123L, optionId, "채널 주문 재고", null, null, "PAYED",
                null, null, null, null, quantity, quantity, CHANGED_AT.minusMinutes(1), null,
                null, null, null, null, null, null, null, null, null, List.of());
    }

    private static ProductOrderChange change(String id) {
        return new ProductOrderChange(id, "PAYED", CHANGED_AT);
    }
}
