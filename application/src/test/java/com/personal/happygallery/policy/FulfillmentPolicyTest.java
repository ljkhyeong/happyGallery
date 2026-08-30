package com.personal.happygallery.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.order.FulfillmentPolicy;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.ShipmentTrackingStatus;
import com.personal.happygallery.domain.order.ShippingCarrier;
import com.personal.happygallery.domain.order.TrackingRegistrationStatus;
import com.personal.happygallery.domain.order.ShippingAddress;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("policy")
class FulfillmentPolicyTest {

    private static final ShippingAddress SHIPPING_ADDRESS = new ShippingAddress(
            "홍길동", "01012345678", "06236", "서울시 강남구 테헤란로 1", null);

    @Test
    @DisplayName("배송은 배송지를 요구하고 픽업은 배송지를 허용하지 않는다")
    void requireValid_rejectsMismatchedFulfillmentAndAddress() {
        assertInvalid(
                () -> FulfillmentPolicy.requireValid(null, null),
                "수령 방법을 선택해 주세요.");
        assertInvalid(
                () -> FulfillmentPolicy.requireValid(FulfillmentType.SHIPPING, null),
                "배송지는 필수입니다.");
        assertInvalid(
                () -> FulfillmentPolicy.requireValid(FulfillmentType.PICKUP, SHIPPING_ADDRESS),
                "픽업 주문에는 배송지를 입력할 수 없습니다.");
    }

    @Test
    @DisplayName("배송지와 수령 방법이 일치하면 주문 이행을 허용한다")
    void requireValid_acceptsMatchingFulfillmentAndAddress() {
        assertThatCode(() -> FulfillmentPolicy.requireValid(
                FulfillmentType.SHIPPING, SHIPPING_ADDRESS))
                .doesNotThrowAnyException();
        assertThatCode(() -> FulfillmentPolicy.requireValid(FulfillmentType.PICKUP, null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("지원 택배사 운송장을 기록하면 배송조회 등록 대기 상태가 된다")
    void recordShipment_startsTrackingRegistration() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 27, 15, 0);
        Fulfillment fulfillment = Fulfillment.shipping(1L, "encrypted-address");

        fulfillment.recordShipment(ShippingCarrier.CJ_LOGISTICS, " 123-456 ", now);

        assertThat(fulfillment.getCarrier()).isEqualTo("CJ대한통운");
        assertThat(fulfillment.getTrackingNumber()).isEqualTo("123-456");
        assertThat(fulfillment.getCarrierCode()).isEqualTo(ShippingCarrier.CJ_LOGISTICS);
        assertThat(fulfillment.getTrackingRegistrationStatus())
                .isEqualTo(TrackingRegistrationStatus.PENDING);
        assertThat(fulfillment.getTrackingStatus()).isEqualTo(ShipmentTrackingStatus.PENDING);
        assertThat(fulfillment.getTrackingNextAttemptAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("택배사 배송 완료는 배송조회 상태만 완료하고 주문 상태는 다루지 않는다")
    void applyTrackingUpdate_completesTrackingOnly() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 27, 15, 0);
        Fulfillment fulfillment = Fulfillment.shipping(1L, "encrypted-address");
        fulfillment.recordShipment(ShippingCarrier.HANJIN, "1234567890", now);
        fulfillment.claimTrackingRegistration(now, now.minusMinutes(5));

        fulfillment.applyTrackingUpdate(ShipmentTrackingStatus.DELIVERED, "배송완료", now.plusHours(2));
        fulfillment.completeTrackingRegistration("request-1", now.plusHours(2));

        assertThat(fulfillment.getTrackingStatus()).isEqualTo(ShipmentTrackingStatus.DELIVERED);
        assertThat(fulfillment.getTrackingRegistrationStatus())
                .isEqualTo(TrackingRegistrationStatus.COMPLETED);
    }

    private static void assertInvalid(Runnable validation, String message) {
        assertThatThrownBy(validation::run)
                .isInstanceOfSatisfying(HappyGalleryException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
                    assertThat(exception.getMessage()).isEqualTo(message);
                });
    }
}
