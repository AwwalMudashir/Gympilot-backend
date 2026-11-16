package com.project.gympilot_backend.workout.dto;

import com.project.gympilot_backend.workout.model.WorkoutPlanItem;
import com.project.gympilot_backend.workout.service.CalorieCalculator;
import java.math.BigDecimal;
import java.util.UUID;

public record WorkoutPlanItemResponse(
        UUID id,
        ExerciseResponse exercise,
        int sets,
        int targetRepetitions,
        int restSeconds,
        int order,
        boolean cameraSupported,
        BigDecimal estimatedCalories
) {
    public static WorkoutPlanItemResponse from(WorkoutPlanItem item, BigDecimal weightKg) {
        return new WorkoutPlanItemResponse(
                item.getId(), ExerciseResponse.from(item.getExercise(), weightKg), item.getSets(),
                item.getTargetRepetitions(), item.getRestSeconds(), item.getItemOrder(),
                item.isCameraSupported(), CalorieCalculator.estimatePlanItem(item, weightKg));
    }
}
