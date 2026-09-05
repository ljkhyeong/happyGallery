package com.personal.happygallery.adapter.in.web.inquiry.dto;

import com.personal.happygallery.domain.inquiry.GroupInquiryDetails;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GroupInquiryRequest(
        @NotBlank @Size(max = 200) String organization,
        @NotBlank @Size(max = 100) String contactName,
        @NotBlank @Size(max = 30) String phone,
        @Schema(nullable = true) @Size(max = 254) String email,
        @NotNull @Min(1) @Max(500) Integer headcount,
        @NotBlank @Size(max = 200) String preferredSchedule,
        @NotBlank @Size(max = 200) String location,
        @NotBlank @Size(max = 200) String classInterest,
        @Schema(nullable = true) @Size(max = 2000) String message) {
    public GroupInquiryDetails toDetails() {
        return new GroupInquiryDetails(organization, contactName, phone, email, headcount,
                preferredSchedule, location, classInterest, message);
    }
    public static GroupInquiryRequest from(GroupInquiryDetails details) {
        return new GroupInquiryRequest(details.organization(), details.contactName(), details.phone(), details.email(),
                details.headcount(), details.preferredSchedule(), details.location(), details.classInterest(), details.message());
    }
}
