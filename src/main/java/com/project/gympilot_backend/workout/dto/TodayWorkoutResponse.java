package com.project.gympilot_backend.workout.dto;

import java.time.LocalDate;
import java.util.UUID;

public record TodayWorkoutResponse(
        UUID planId,
        LocalDate date,
        boolean restDay,
        WorkoutDayResponse workout
) {}
