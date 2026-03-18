package com.personal.happygallery.app.web.customer.dto;

import java.util.List;

public record ClaimGuestRecordsRequest(
        List<Long> orderIds,
        List<Long> bookingIds,
        List<Long> passIds) {}
