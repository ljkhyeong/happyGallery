package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.out.ClassReaderPort;
import com.personal.happygallery.application.booking.port.out.ClassStorePort;
import com.personal.happygallery.domain.booking.BookingClass;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("local")
public class LocalBookingClassSeedService {

    private static final Logger log = LoggerFactory.getLogger(LocalBookingClassSeedService.class);

    private final ClassReaderPort classReaderPort;
    private final ClassStorePort classStorePort;

    public LocalBookingClassSeedService(ClassReaderPort classReaderPort,
                                         ClassStorePort classStorePort) {
        this.classReaderPort = classReaderPort;
        this.classStorePort = classStorePort;
    }

    @Transactional
    public void seedIfEmpty() {
        if (classReaderPort.count() > 0) {
            return;
        }

        List<BookingClass> defaultClasses = List.of(
                new BookingClass("향수 클래스", "PERFUME", 120, 50_000L, 30),
                new BookingClass("우드 클래스", "WOOD", 90, 42_000L, 30),
                new BookingClass("니트 클래스", "KNIT", 120, 45_000L, 30)
        );

        classStorePort.saveAll(defaultClasses);
        log.info("[LocalSeed] 기본 클래스 {}건을 생성했습니다.", defaultClasses.size());
    }
}
