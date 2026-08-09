package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.in.ClassQueryUseCase;
import com.personal.happygallery.application.booking.port.out.ClassReaderPort;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.error.NotFoundException;
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
    @Override
    public List<BookingClass> listAll() {
        return classReaderPort.findAll();
    }

    /** 공개 가능한 활성 클래스 목록 조회 */
    @Override
    public List<BookingClass> listActive() {
        return classReaderPort.findAllActive();
    }

    /** 공개 클래스 단건 조회 — 존재 여부와 현재 운영 상태를 함께 확인한다. */
    @Override
    public BookingClass getActive(Long id) {
        BookingClass bookingClass = classReaderPort.findById(id)
                .orElseThrow(NotFoundException.supplier("클래스"));
        bookingClass.requireActive();
        return bookingClass;
    }
}
