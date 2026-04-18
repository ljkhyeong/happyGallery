package com.personal.happygallery.application.dashboard.dto;

import java.time.LocalDate;

public record SlotUtilization(
        LocalDate date,
        String className,
        int totalCapacity,
        int totalBooked,
        double utilizationRate
) {}
