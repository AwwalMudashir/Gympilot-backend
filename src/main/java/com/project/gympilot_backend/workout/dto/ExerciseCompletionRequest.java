package com.project.gympilot_backend.workout.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ExerciseCompletionRequest(
        @NotNull UUID exerciseId,
        @NotNull @Min(0) Integer completedSets,
        @NotNull @Min(0) Integer completedRepetitions
) {
}
