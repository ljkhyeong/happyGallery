package com.personal.happygallery.app.booking;

import com.personal.happygallery.app.booking.port.out.ClassReaderPort;
import com.personal.happygallery.app.booking.port.out.ClassStorePort;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.infra.booking.ClassRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * {@link ClassRepository}(infra) → {@link ClassReaderPort} + {@link ClassStorePort}(app) 브릿지 어댑터.
 */
@Component
class ClassPersistencePortAdapter implements ClassReaderPort, ClassStorePort {

    private final ClassRepository classRepository;

    ClassPersistencePortAdapter(ClassRepository classRepository) {
        this.classRepository = classRepository;
    }

    @Override
    public Optional<BookingClass> findById(Long id) {
        return classRepository.findById(id);
    }

    @Override
    public List<BookingClass> findAll() {
        return classRepository.findAll();
    }

    @Override
    public long count() {
        return classRepository.count();
    }

    @Override
    public BookingClass save(BookingClass bookingClass) {
        return classRepository.save(bookingClass);
    }

    @Override
    public List<BookingClass> saveAll(List<BookingClass> classes) {
        return classRepository.saveAll(classes);
    }
}
