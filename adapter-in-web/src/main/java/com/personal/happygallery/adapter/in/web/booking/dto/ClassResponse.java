package com.personal.happygallery.adapter.in.web.booking.dto;

import com.personal.happygallery.domain.booking.BookingClass;

public record ClassResponse(
        Long id,
        String name,
        String category,
        int durationMin,
        long price,
        int bufferMin
) {
    public static ClassResponse from(BookingClass bc) {
        return new ClassResponse(
                bc.getId(),
                bc.getName(),
                bc.getCategory(),
                bc.getDurationMin(),
                bc.getPrice(),
                bc.getBufferMin()
        );
    }
}
