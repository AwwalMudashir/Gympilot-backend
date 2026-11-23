package com.project.gympilot_backend.wellness.progress.dto;

import java.time.LocalDate;
import java.util.List;

public record WellnessProgressResponse(
        LocalDate fromDate,
        LocalDate toDate,
        long averageSleepMinutes,
        double averageEnergyLevel,
        int hydrationGoalDays,
        int totalNutritionCalories,
        List<WellnessDayResponse> dailyBreakdown
) {
}
