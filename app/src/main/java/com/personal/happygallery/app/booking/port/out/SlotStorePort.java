package com.personal.happygallery.app.booking.port.out;

import com.personal.happygallery.domain.booking.Slot;

public interface SlotStorePort {
    Slot save(Slot slot);
}
