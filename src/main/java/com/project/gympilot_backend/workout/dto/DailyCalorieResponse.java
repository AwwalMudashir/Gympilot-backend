package com.project.gympilot_backend.workout.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyCalorieResponse(
        LocalDate date,
        BigDecimal caloriesBurned,
        int completedWorkouts
) {
}
