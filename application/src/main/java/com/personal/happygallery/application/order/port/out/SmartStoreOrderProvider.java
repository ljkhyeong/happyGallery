package com.personal.happygallery.application.order.port.out;

import com.personal.happygallery.application.order.dto.SmartStoreClaimDetail;
import com.personal.happygallery.application.order.dto.SmartStoreDelayCommand;
import com.personal.happygallery.application.order.dto.SmartStoreDeliveryInfo;
import com.personal.happygallery.application.order.dto.SmartStoreDispatchCommand;
import com.personal.happygallery.application.order.dto.SmartStoreExchangeDispatchCommand;
import com.personal.happygallery.application.order.dto.SmartStoreExchangeHoldCommand;
import com.personal.happygallery.application.order.dto.SmartStoreExchangeRejectCommand;
import com.personal.happygallery.application.order.dto.SmartStoreReturnHoldCommand;
import com.personal.happygallery.application.order.dto.SmartStoreSellerCancelCommand;
import com.personal.happygallery.application.order.dto.SmartStoreSellerReturnCommand;
import java.time.LocalDateTime;
import java.util.List;

public interface SmartStoreOrderProvider {

    boolean isEnabled();

    List<ReturnDeliveryCompany> findReturnDeliveryCompanies();

    record ReturnDeliveryCompany(long id, String name, String returnDeliveryCompanyPriorityType) {}

    ChangePage fetchChanges(ChangeCursor cursor, LocalDateTime changedTo);

    List<ProductOrderDetail> fetchDetails(List<String> productOrderIds);

    default void confirm(String productOrderId) {
        confirmAll(List.of(productOrderId)).requireSuccess(productOrderId);
    }

    OperationResult confirmAll(List<String> productOrderIds);

    default void dispatch(SmartStoreDispatchCommand command) {
        dispatchAll(List.of(command)).requireSuccess(command.productOrderId());
    }

    OperationResult dispatchAll(List<SmartStoreDispatchCommand> commands);

    void delay(SmartStoreDelayCommand command);

    void approveCancel(String productOrderId);

    void approveReturn(String productOrderId);

    void rejectReturn(String productOrderId);

    void holdReturn(SmartStoreReturnHoldCommand command);

    void releaseReturnHold(String productOrderId);

    void requestSellerReturn(SmartStoreSellerReturnCommand command);

    void dispatchExchange(SmartStoreExchangeDispatchCommand command);

    void completeExchangeCollect(String productOrderId);

    void rejectExchange(SmartStoreExchangeRejectCommand command);

    void holdExchange(SmartStoreExchangeHoldCommand command);

    void releaseExchangeHold(String productOrderId);

    void requestSellerCancel(SmartStoreSellerCancelCommand command);

    record ChangeCursor(LocalDateTime changedFrom, String moreSequence) {}

    record ChangePage(List<ProductOrderChange> changes, ChangeCursor nextCursor) {
        public ChangePage {
            changes = List.copyOf(changes);
        }
    }

    record ProductOrderChange(
            String productOrderId,
            String lastChangedType,
            LocalDateTime lastChangedAt
    ) {}

    record ProductOrderDetail(
            String productOrderId,
            String orderId,
            Long originProductNo,
            Long itemNo,
            String productName,
            String productOption,
            SmartStoreDeliveryInfo deliveryInfo,
            String productOrderStatus,
            String placeOrderStatus,
            String claimType,
            String claimStatus,
            SmartStoreClaimDetail claimDetail,
            int initialQuantity,
            int remainQuantity,
            LocalDateTime paymentDate,
            LocalDateTime shippingDueDate,
            String expectedDeliveryMethod,
            String deliveryCompany,
            String trackingNumber,
            Long unitPrice,
            Long paymentAmount,
            Long paymentCommission,
            Long saleCommission,
            Long channelCommission,
            Long expectedSettlementAmount,
            List<CompletedReturn> completedReturns
    ) {
        public int completedReturnQuantity() {
            return completedReturns.stream().mapToInt(CompletedReturn::quantity).sum();
        }
    }

    record CompletedReturn(String claimId, int quantity, LocalDateTime completedAt) {}

    record OperationResult(List<String> successProductOrderIds, List<OperationFailure> failures) {
        public OperationResult {
            successProductOrderIds = successProductOrderIds == null
                    ? List.of() : List.copyOf(successProductOrderIds);
            failures = failures == null ? List.of() : List.copyOf(failures);
        }

        public void requireSuccess(String productOrderId) {
            failures.stream()
                    .filter(failure -> productOrderId.equals(failure.productOrderId()))
                    .findFirst()
                    .ifPresent(failure -> {
                        if ("UNKNOWN_RESULT".equals(failure.code())) {
                            throw new OperationResultUnknownException(failure.message());
                        }
                        throw new OperationRejectedException(failure.code(), failure.message());
                    });
        }
    }

    record OperationFailure(String productOrderId, String code, String message) {}

    class OperationRejectedException extends RuntimeException {
        private final String code;

        public OperationRejectedException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String code() {
            return code;
        }
    }

    class OperationNotSentException extends RuntimeException {
        private final String code;

        public OperationNotSentException(String code, String message, Throwable cause) {
            super(message, cause);
            this.code = code;
        }

        public String code() {
            return code;
        }
    }

    class OperationResultUnknownException extends RuntimeException {
        public OperationResultUnknownException(String message) {
            super(message);
        }

        public OperationResultUnknownException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
