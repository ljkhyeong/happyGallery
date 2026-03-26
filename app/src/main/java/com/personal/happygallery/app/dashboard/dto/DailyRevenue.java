package com.personal.happygallery.app.dashboard.dto;

import java.time.LocalDate;

public record DailyRevenue(
        LocalDate date,
        long revenue
) {}
