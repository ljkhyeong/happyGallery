package com.personal.happygallery.app.booking.port.in;

import com.personal.happygallery.domain.booking.BookingClass;
import java.util.List;

/**
 * 클래스 조회 유스케이스.
 */
public interface ClassQueryUseCase {

    List<BookingClass> listAll();
}
