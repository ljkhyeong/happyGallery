package com.personal.happygallery.application.booking.port.in;

import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.BookingClassStatus;

/**
 * 클래스 관리 유스케이스.
 */
public interface ClassManagementUseCase {

    record CreateClassCommand(
            String name,
            String category,
            int durationMin,
            long price,
            int bufferMin,
            int capacity,
            boolean passEligible,
            String description,
            String imageUrl,
            String preparationInfo,
            String targetAudience
    ) {}

    record UpdateClassCommand(
            Long classId,
            String name,
            String category,
            long price,
            boolean passEligible,
            String description,
            String imageUrl,
            String preparationInfo,
            String targetAudience
    ) {}

    BookingClass createClass(CreateClassCommand command);

    BookingClass updateClass(UpdateClassCommand command);

    BookingClass changeStatus(Long classId, BookingClassStatus status);
}
