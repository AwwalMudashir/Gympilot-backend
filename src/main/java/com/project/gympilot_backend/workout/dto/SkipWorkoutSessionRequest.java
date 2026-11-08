package com.project.gympilot_backend.workout.dto;

import jakarta.validation.constraints.Size;

public record SkipWorkoutSessionRequest(@Size(max = 500) String notes) {
}
