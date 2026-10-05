package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.ClassSituationTag;
import com.personal.happygallery.domain.payment.PaymentAmountPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateClassRequest(
        @NotBlank @Size(max = BookingClass.MAX_NAME_LENGTH) String name,
        @NotBlank @Size(max = 30) String category,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @Positive int durationMin,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @Min(BookingClass.MIN_PRICE) @Max(PaymentAmountPolicy.MAX_AMOUNT) long price,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @PositiveOrZero int bufferMin,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1")
        @Positive int capacity,
        @NotNull Boolean passEligible,
        @Size(max = BookingClass.MAX_DESCRIPTION_LENGTH) String description,
        @Size(max = BookingClass.MAX_IMAGE_URL_LENGTH) String imageUrl,
        @Size(max = BookingClass.MAX_PREPARATION_INFO_LENGTH) String preparationInfo,
        @Size(max = BookingClass.MAX_TARGET_AUDIENCE_LENGTH) String targetAudience,
        @Schema(description = "고객이 상황으로 수업을 찾을 때 쓰는 태그. 생략하면 태그 없이 만든다.", nullable = true)
        List<ClassSituationTag> situationTags
) {
}
