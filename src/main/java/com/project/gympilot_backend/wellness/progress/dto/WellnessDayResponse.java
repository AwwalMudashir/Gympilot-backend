package com.project.gympilot_backend.wellness.progress.dto;

import com.project.gympilot_backend.wellness.sleep.model.SleepQuality;
import java.math.BigDecimal;
import java.time.LocalDate;

public record WellnessDayResponse(
        LocalDate date,
        long sleepMinutes,
        SleepQuality sleepQuality,
        Integer energyLevel,
        int waterMl,
        int waterTargetMl,
        int nutritionCalories,
        BigDecimal proteinGrams,
        BigDecimal carbohydrateGrams,
        BigDecimal fatGrams
) {
}
