package com.project.gympilot_backend.workout.dto;

import com.project.gympilot_backend.vision.model.ExerciseResult;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public record ExerciseResultResponse(
        UUID exerciseId,
        String exerciseName,
        Integer plannedSets,
        Integer completedSets,
        Integer plannedRepetitions,
        Integer completedRepetitions,
        boolean skipped,
        BigDecimal caloriesBurned,
        BigDecimal averageTempoSeconds,
        BigDecimal formScore,
        List<String> formFlags
) {
    public static ExerciseResultResponse from(ExerciseResult result) {
        return new ExerciseResultResponse(
                result.getExercise().getId(), result.getExercise().getName(),
                result.getPlannedSets(), result.getCompletedSets(),
                result.getPlannedRepetitions(), result.getCompletedRepetitions(),
                result.isSkipped(), result.getCaloriesBurned(),
                result.getAverageRepDurationSeconds(), result.getFormScore(),
                splitFormFlags(result.getFormWarnings()));
    }

    private static List<String> splitFormFlags(String formWarnings) {
        if (formWarnings == null || formWarnings.isBlank()) return List.of();
        return Arrays.stream(formWarnings.split("\\|"))
                .filter(value -> !value.isBlank())
                .toList();
    }
}
