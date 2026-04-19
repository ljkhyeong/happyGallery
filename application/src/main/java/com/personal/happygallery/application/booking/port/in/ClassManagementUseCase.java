package com.personal.happygallery.application.booking.port.in;

import com.personal.happygallery.domain.booking.BookingClass;

/**
 * 클래스 관리 유스케이스.
 */
public interface ClassManagementUseCase {

    BookingClass createClass(String name, String category, int durationMin, long price, int bufferMin);
}
