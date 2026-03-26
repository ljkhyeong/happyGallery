package com.personal.happygallery.app.dashboard.dto;

import java.time.LocalDate;

public record SlotUtilization(
        LocalDate date,
        String className,
        int totalCapacity,
        int totalBooked,
        double utilizationRate
) {}
