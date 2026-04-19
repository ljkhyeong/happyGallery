package com.personal.happygallery.application.dashboard.dto;

import java.time.LocalDate;

public record DailyRevenue(
        LocalDate date,
        long revenue
) {}
