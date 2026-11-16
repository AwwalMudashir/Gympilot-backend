package com.project.gympilot_backend.workout.dto;

import com.project.gympilot_backend.workout.model.Exercise;
import com.project.gympilot_backend.workout.service.CalorieCalculator;
import java.math.BigDecimal;
import java.util.UUID;

public record ExerciseResponse(
        UUID id,
        String code,
        String name,
        String instructions,
        String muscleGroup,
        String equipment,
        boolean cameraSupported,
        BigDecimal estimatedCaloriesPer10Minutes
) {
    public static ExerciseResponse from(Exercise exercise) {
        return from(exercise, BigDecimal.valueOf(70));
    }

    public static ExerciseResponse from(Exercise exercise, BigDecimal weightKg) {
        return new ExerciseResponse(
                exercise.getId(), exercise.getCode(), exercise.getName(), exercise.getInstructions(),
                exercise.getMuscleGroup(), exercise.getEquipment(), exercise.isCameraSupported(),
                CalorieCalculator.estimate(exercise, weightKg, 10 * 60));
    }
}
