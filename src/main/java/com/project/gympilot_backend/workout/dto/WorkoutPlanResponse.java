package com.project.gympilot_backend.workout.dto;

import com.project.gympilot_backend.workout.model.PlanStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record WorkoutPlanResponse(
        UUID id,
        LocalDate weekStartDate,
        PlanStatus status,
        String explanation,
        int workoutDays,
        int totalExercises,
        List<WorkoutDayResponse> days
) {}
