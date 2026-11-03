package com.project.gympilot_backend.workout.dto;

import com.project.gympilot_backend.workout.model.WorkoutPlanItem;
import java.util.UUID;

public record WorkoutPlanItemResponse(
        UUID id,
        ExerciseResponse exercise,
        int sets,
        int targetRepetitions,
        int restSeconds,
        int order,
        boolean cameraSupported
) {
    public static WorkoutPlanItemResponse from(WorkoutPlanItem item) {
        return new WorkoutPlanItemResponse(
                item.getId(), ExerciseResponse.from(item.getExercise()), item.getSets(),
                item.getTargetRepetitions(), item.getRestSeconds(), item.getItemOrder(),
                item.isCameraSupported());
    }
}
