package com.project.gympilot_backend.workout.dto;

import com.project.gympilot_backend.workout.model.Exercise;
import java.util.UUID;

public record ExerciseResponse(
        UUID id,
        String code,
        String name,
        String instructions,
        String muscleGroup,
        String equipment,
        boolean cameraSupported
) {
    public static ExerciseResponse from(Exercise exercise) {
        return new ExerciseResponse(
                exercise.getId(), exercise.getCode(), exercise.getName(), exercise.getInstructions(),
                exercise.getMuscleGroup(), exercise.getEquipment(), exercise.isCameraSupported());
    }
}
