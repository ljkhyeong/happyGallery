package com.personal.happygallery.application.payment.context.order;

import com.personal.happygallery.application.payment.context.PaymentPreparer;
import com.personal.happygallery.application.payment.port.in.AuthContext;
import com.personal.happygallery.application.payment.port.in.PaymentPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderItemRef;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderPayload;
import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.payment.PaymentContext;
import com.personal.happygallery.domain.product.Product;
import org.springframework.stereotype.Component;

@Component
public class OrderPreparer implements PaymentPreparer {

    private final ProductReaderPort productReader;

    public OrderPreparer(ProductReaderPort productReader) {
        this.productReader = productReader;
    }

    @Override
    public PaymentContext context() {
        return PaymentContext.ORDER;
    }

    @Override
    public long calculateAmount(PaymentPayload payload, AuthContext auth) {
        if (!(payload instanceof OrderPayload op)) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "주문 결제 payload가 아닙니다.");
        }
        if (op.items() == null || op.items().isEmpty()) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "주문 항목이 비었습니다.");
        }
        if (auth.isMember()) {
            if (op.userId() == null || !op.userId().equals(auth.userId())) {
                throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "회원 정보가 인증과 일치하지 않습니다.");
            }
        } else {
            if (op.phone() == null || op.verificationCode() == null || op.name() == null) {
                throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "비회원 주문은 휴대폰 인증이 필요합니다.");
            }
        }

        long total = 0;
        for (OrderItemRef item : op.items()) {
            if (item.qty() <= 0) {
                throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "주문 수량은 1 이상이어야 합니다.");
            }
            Product product = productReader.findById(item.productId())
                    .orElseThrow(NotFoundException.supplier("상품"));
            total += (long) item.qty() * product.getPrice();
        }
        return total;
    }
}
