package com.project.gympilot_backend.workout.dto;

import com.project.gympilot_backend.vision.model.ExerciseResult;
import java.util.UUID;

public record ExerciseResultResponse(
        UUID exerciseId,
        String exerciseName,
        Integer plannedSets,
        Integer completedSets,
        Integer plannedRepetitions,
        Integer completedRepetitions
) {
    public static ExerciseResultResponse from(ExerciseResult result) {
        return new ExerciseResultResponse(
                result.getExercise().getId(), result.getExercise().getName(),
                result.getPlannedSets(), result.getCompletedSets(),
                result.getPlannedRepetitions(), result.getCompletedRepetitions());
    }
}
