package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.in.ClassManagementUseCase;
import com.personal.happygallery.application.booking.port.out.ClassReaderPort;
import com.personal.happygallery.application.booking.port.out.ClassStorePort;
import com.personal.happygallery.application.media.ImageMediaReferenceGuard;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.BookingClassStatus;
import com.personal.happygallery.domain.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultClassManagementService implements ClassManagementUseCase {

    private final ClassStorePort classStorePort;
    private final ClassReaderPort classReaderPort;
    private final ImageMediaReferenceGuard imageMediaReferenceGuard;

    public DefaultClassManagementService(ClassStorePort classStorePort,
                                         ClassReaderPort classReaderPort,
                                         ImageMediaReferenceGuard imageMediaReferenceGuard) {
        this.classStorePort = classStorePort;
        this.classReaderPort = classReaderPort;
        this.imageMediaReferenceGuard = imageMediaReferenceGuard;
    }

    @Override
    public BookingClass createClass(CreateClassCommand command) {
        BookingClass bookingClass = new BookingClass(
                command.name(),
                command.category(),
                command.durationMin(),
                command.price(),
                command.bufferMin(),
                command.passEligible(),
                command.description(),
                command.imageUrl(),
                command.preparationInfo(),
                command.targetAudience());
        imageMediaReferenceGuard.validateAssignment(bookingClass.getImageUrl());
        return classStorePort.save(bookingClass);
    }

    @Override
    public BookingClass updateClass(UpdateClassCommand command) {
        BookingClass bookingClass = classReaderPort.findByIdForUpdate(command.classId())
                .orElseThrow(NotFoundException.supplier("클래스"));
        bookingClass.updateDetails(
                command.name(),
                command.category(),
                command.price(),
                command.passEligible(),
                command.description(),
                command.imageUrl(),
                command.preparationInfo(),
                command.targetAudience());
        imageMediaReferenceGuard.validateAssignment(bookingClass.getImageUrl());
        return classStorePort.save(bookingClass);
    }

    @Override
    public BookingClass changeStatus(Long classId, BookingClassStatus status) {
        BookingClass bookingClass = classReaderPort.findByIdForUpdate(classId)
                .orElseThrow(NotFoundException.supplier("클래스"));
        bookingClass.changeStatus(status);
        return classStorePort.save(bookingClass);
    }
}
