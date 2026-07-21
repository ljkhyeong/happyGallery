package com.personal.happygallery.application.booking.port.out;

import com.personal.happygallery.domain.booking.BookingClass;
import java.util.List;
import java.util.Optional;

public interface ClassReaderPort {

    Optional<BookingClass> findById(Long id);

    /** 같은 클래스의 슬롯 생성·예약 범위를 직렬화하기 위해 클래스 행을 잠근다. */
    Optional<BookingClass> findByIdForUpdate(Long id);

    /** 여러 클래스 행을 PK 오름차순으로 잠근다. */
    List<BookingClass> findAllByIdForUpdate(List<Long> ids);

    List<BookingClass> findAll();

    List<BookingClass> findAllActive();

    long count();
}
