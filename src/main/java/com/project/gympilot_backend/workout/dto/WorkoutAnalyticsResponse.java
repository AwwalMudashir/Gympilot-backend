package com.project.gympilot_backend.workout.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record WorkoutAnalyticsResponse(
        LocalDate weekStartDate,
        BigDecimal todayCalories,
        BigDecimal weekCalories,
        int completedWorkouts,
        int completedExercises,
        List<DailyCalorieResponse> dailyBreakdown,
        List<ExerciseCalorieResponse> exerciseBreakdown
) {
}
