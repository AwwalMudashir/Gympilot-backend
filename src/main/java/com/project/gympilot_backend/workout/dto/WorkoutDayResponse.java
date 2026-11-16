package com.project.gympilot_backend.workout.dto;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;

public record WorkoutDayResponse(
        DayOfWeek day,
        String title,
        int estimatedMinutes,
        BigDecimal estimatedCalories,
        List<WorkoutPlanItemResponse> exercises
) {}
