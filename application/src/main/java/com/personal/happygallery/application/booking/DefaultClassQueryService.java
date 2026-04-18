package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.in.ClassQueryUseCase;
import com.personal.happygallery.application.booking.port.out.ClassReaderPort;
import com.personal.happygallery.domain.booking.BookingClass;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DefaultClassQueryService implements ClassQueryUseCase {

    private final ClassReaderPort classReaderPort;

    public DefaultClassQueryService(ClassReaderPort classReaderPort) {
        this.classReaderPort = classReaderPort;
    }

    /** 전체 클래스 목록 조회 */
    public List<BookingClass> listAll() {
        return classReaderPort.findAll();
    }
}
