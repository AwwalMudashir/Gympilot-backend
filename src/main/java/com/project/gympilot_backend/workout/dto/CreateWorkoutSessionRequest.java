package com.project.gympilot_backend.workout.dto;

import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;

public record CreateWorkoutSessionRequest(
        @NotNull UUID workoutPlanId,
        @NotNull DayOfWeek workoutDay,
        @NotNull LocalDate scheduledDate
) {
}
