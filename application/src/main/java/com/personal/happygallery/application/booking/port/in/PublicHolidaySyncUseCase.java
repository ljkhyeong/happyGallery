package com.personal.happygallery.application.booking.port.in;

import com.personal.happygallery.application.batch.BatchResult;

public interface PublicHolidaySyncUseCase {

    BatchResult syncAnnualSnapshots();
}
