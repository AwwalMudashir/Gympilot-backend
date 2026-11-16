package com.project.gympilot_backend.workout.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ExerciseCalorieResponse(
        UUID exerciseId,
        String exerciseName,
        BigDecimal caloriesBurned,
        int completedSets
) {
}
