package com.project.gympilot_backend.workout.controller;

import com.project.gympilot_backend.common.response.ApiResponse;
import com.project.gympilot_backend.security.service.CurrentUserService;
import com.project.gympilot_backend.workout.dto.WorkoutAnalyticsResponse;
import com.project.gympilot_backend.workout.service.WorkoutAnalyticsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workouts/analytics")
public class WorkoutAnalyticsController {
    private final CurrentUserService currentUserService;
    private final WorkoutAnalyticsService analyticsService;

    public WorkoutAnalyticsController(
            CurrentUserService currentUserService,
            WorkoutAnalyticsService analyticsService
    ) {
        this.currentUserService = currentUserService;
        this.analyticsService = analyticsService;
    }

    @GetMapping
    public ApiResponse<WorkoutAnalyticsResponse> currentWeek(
            Authentication authentication,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                "Workout analytics retrieved",
                analyticsService.currentWeek(currentUserService.require(authentication)),
                request.getRequestURI());
    }
}
