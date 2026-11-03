package com.project.gympilot_backend.workout.dto;

import java.time.DayOfWeek;
import java.util.List;

public record WorkoutDayResponse(
        DayOfWeek day,
        String title,
        int estimatedMinutes,
        List<WorkoutPlanItemResponse> exercises
) {}
