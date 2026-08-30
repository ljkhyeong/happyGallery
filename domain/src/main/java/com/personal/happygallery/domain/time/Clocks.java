package com.personal.happygallery.domain.time;

import java.time.ZoneId;

public final class Clocks {
    public static final String SEOUL_ID = "Asia/Seoul";
    public static final ZoneId SEOUL = ZoneId.of(SEOUL_ID);

    private Clocks() {}
}
