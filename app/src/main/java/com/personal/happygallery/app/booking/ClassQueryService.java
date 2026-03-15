package com.personal.happygallery.app.booking;

import com.personal.happygallery.app.booking.port.out.ClassReaderPort;
import com.personal.happygallery.domain.booking.BookingClass;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ClassQueryService {

    private final ClassReaderPort classReaderPort;

    public ClassQueryService(ClassReaderPort classReaderPort) {
        this.classReaderPort = classReaderPort;
    }

    /** 전체 클래스 목록 조회 */
    public List<BookingClass> listAll() {
        return classReaderPort.findAll();
    }
}
