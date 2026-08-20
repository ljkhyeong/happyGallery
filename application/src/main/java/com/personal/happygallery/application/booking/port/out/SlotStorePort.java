package com.personal.happygallery.application.booking.port.out;

import com.personal.happygallery.domain.booking.Slot;
import java.util.List;

public interface SlotStorePort {

    <S extends Slot> S save(S slot);

    <S extends Slot> List<S> saveAll(Iterable<S> slots);
}
