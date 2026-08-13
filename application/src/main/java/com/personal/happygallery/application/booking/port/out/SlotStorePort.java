package com.personal.happygallery.application.booking.port.out;

import com.personal.happygallery.domain.booking.Slot;

public interface SlotStorePort {

    <S extends Slot> S save(S slot);
}
