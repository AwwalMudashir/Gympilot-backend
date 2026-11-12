package com.project.gympilot_backend.workout.dto;

import com.project.gympilot_backend.workout.model.DifficultyRating;
import com.project.gympilot_backend.workout.model.SessionStatus;
import com.project.gympilot_backend.workout.model.WorkoutPhase;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record WorkoutSessionResponse(
        UUID id,
        UUID workoutPlanId,
        DayOfWeek workoutDay,
        String title,
        LocalDate scheduledDate,
        Instant startedAt,
        Instant completedAt,
        Instant activeSince,
        SessionStatus status,
        DifficultyRating difficultyRating,
        String notes,
        Integer currentExerciseIndex,
        Integer currentSetNumber,
        WorkoutPhase currentPhase,
        Integer phaseRemainingSeconds,
        Integer elapsedSeconds,
        List<ExerciseResultResponse> results
) {
}
