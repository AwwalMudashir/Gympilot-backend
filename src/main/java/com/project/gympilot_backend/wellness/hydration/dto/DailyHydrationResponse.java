package com.project.gympilot_backend.wellness.hydration.dto;

import java.time.LocalDate;
import java.util.List;

public record DailyHydrationResponse(
        LocalDate date,
        int totalMl,
        int targetMl,
        int progressPercent,
        List<WaterLogResponse> entries
) {
}
