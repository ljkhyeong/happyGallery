package com.personal.happygallery.application.booking.port.out;

import com.personal.happygallery.domain.booking.BookingClass;
import java.util.Optional;

public interface BookingClassLockPort {

    /** 클래스 행을 잠그고 1차 캐시가 아닌 DB 최신 상태로 갱신한다. */
    Optional<BookingClass> lockFresh(Long classId);
}
