package com.project.gympilot_backend.workout.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record RescheduleWorkoutSessionRequest(@NotNull LocalDate scheduledDate) {
}
