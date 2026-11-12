package com.project.gympilot_backend.workout.dto;

import com.project.gympilot_backend.workout.model.WorkoutPhase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record WorkoutProgressRequest(
        @NotNull @Min(0) Integer currentExerciseIndex,
        @NotNull @Min(1) Integer currentSetNumber,
        @NotNull WorkoutPhase currentPhase,
        @NotNull @Min(0) @Max(3600) Integer phaseRemainingSeconds,
        List<@Valid ExerciseCompletionRequest> results
) {
}
