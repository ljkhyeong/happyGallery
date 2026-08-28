package com.personal.happygallery.application.booking.port.out;

import com.personal.happygallery.domain.booking.BookingVacancyAlert;
import java.util.List;
import java.util.Optional;

public interface BookingVacancyAlertPort {

    <S extends BookingVacancyAlert> S save(S alert);

    <S extends BookingVacancyAlert> List<S> saveAll(Iterable<S> alerts);

    Optional<BookingVacancyAlert> findWaitingBySlotIdAndGuestId(Long slotId, Long guestId);

    Optional<BookingVacancyAlert> findWaitingBySlotIdAndUserId(Long slotId, Long userId);

    Optional<BookingVacancyAlert> findWaitingBySlotIdAndAccessTokenHashForUpdate(
            Long slotId, String accessTokenHash);

    Optional<BookingVacancyAlert> findWaitingBySlotIdAndUserIdForUpdate(Long slotId, Long userId);

    List<BookingVacancyAlert> findWaitingBySlotIdForUpdate(Long slotId);
}
