package com.personal.happygallery.application.booking.port.in;

import com.personal.happygallery.domain.booking.BookingVacancyAlert;
import java.util.List;

public interface BookingVacancyAlertUseCase {

    GuestAlertResult registerGuest(GuestAlertCommand command);

    BookingVacancyAlert registerMember(Long slotId, Long userId);

    List<BookingVacancyAlert> listMember(Long userId);

    void cancelGuest(Long slotId, String accessToken);

    void cancelMember(Long slotId, Long userId);

    record GuestAlertCommand(Long slotId, String name, String phone, String verificationCode) {}

    record GuestAlertResult(BookingVacancyAlert alert, String accessToken) {}
}
