package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.store.port.in.WorkshopProfileUseCase;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import org.springframework.stereotype.Component;

@Component
class PublicPaymentAvailabilityGuard {

    private final PublicPaymentProperties properties;
    private final WorkshopProfileUseCase workshopProfileUseCase;

    PublicPaymentAvailabilityGuard(PublicPaymentProperties properties,
                                   WorkshopProfileUseCase workshopProfileUseCase) {
        this.properties = properties;
        this.workshopProfileUseCase = workshopProfileUseCase;
    }

    void requireAvailable() {
        if (properties.requireCompleteBusinessProfile()
                && !workshopProfileUseCase.get().hasCompleteOnlineSalesDisclosure()) {
            throw new HappyGalleryException(
                    ErrorCode.SERVICE_UNAVAILABLE,
                    "온라인 결제를 준비 중입니다. 네이버톡톡 또는 카카오톡으로 문의해 주세요.");
        }
    }
}
