package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.in.ClassManagementUseCase;
import com.personal.happygallery.application.booking.port.out.ClassStorePort;
import com.personal.happygallery.domain.booking.BookingClass;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultClassManagementService implements ClassManagementUseCase {

    private final ClassStorePort classStorePort;

    public DefaultClassManagementService(ClassStorePort classStorePort) {
        this.classStorePort = classStorePort;
    }

    @Override
    public BookingClass createClass(String name, String category, int durationMin, long price, int bufferMin) {
        return classStorePort.save(new BookingClass(
                name.trim(),
                category.trim().toUpperCase(Locale.ROOT),
                durationMin,
                price,
                bufferMin
        ));
    }
}
