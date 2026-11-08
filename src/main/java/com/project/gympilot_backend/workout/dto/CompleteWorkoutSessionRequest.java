package com.project.gympilot_backend.workout.dto;

import com.project.gympilot_backend.workout.model.DifficultyRating;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CompleteWorkoutSessionRequest(
        DifficultyRating difficultyRating,
        @Size(max = 500) String notes,
        List<@Valid ExerciseCompletionRequest> results
) {
}
