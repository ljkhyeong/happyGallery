package com.personal.happygallery.application.booking.port.in;

import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.BookingClassStatus;
import com.personal.happygallery.domain.booking.ClassSituationTag;
import java.util.Set;

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
            String targetAudience,
            /** null이면 태그 없이 만든다. */
            Set<ClassSituationTag> situationTags
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
            String targetAudience,
            /** null이면 기존 태그를 유지한다. 빈 집합은 모든 태그를 지운다. */
            Set<ClassSituationTag> situationTags
    ) {}

    BookingClass createClass(CreateClassCommand command);

    BookingClass updateClass(UpdateClassCommand command);

    BookingClass changeStatus(Long classId, BookingClassStatus status);
}
