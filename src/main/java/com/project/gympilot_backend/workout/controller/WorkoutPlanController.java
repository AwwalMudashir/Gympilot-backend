package com.project.gympilot_backend.workout.controller;

import com.project.gympilot_backend.common.response.ApiResponse;
import com.project.gympilot_backend.security.service.CurrentUserService;
import com.project.gympilot_backend.workout.dto.TodayWorkoutResponse;
import com.project.gympilot_backend.workout.dto.WorkoutPlanResponse;
import com.project.gympilot_backend.workout.service.WorkoutPlanService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workouts")
public class WorkoutPlanController {
    private final CurrentUserService currentUserService;
    private final WorkoutPlanService workoutPlanService;

    public WorkoutPlanController(CurrentUserService currentUserService, WorkoutPlanService workoutPlanService) {
        this.currentUserService = currentUserService;
        this.workoutPlanService = workoutPlanService;
    }

    @GetMapping("/plans/current")
    public ApiResponse<WorkoutPlanResponse> current(Authentication authentication, HttpServletRequest request) {
        return ApiResponse.success("Current workout plan retrieved",
                workoutPlanService.current(currentUserService.require(authentication)), request.getRequestURI());
    }

    @PostMapping("/plans/generate")
    public ApiResponse<WorkoutPlanResponse> generate(Authentication authentication, HttpServletRequest request) {
        return ApiResponse.success("Weekly workout plan ready",
                workoutPlanService.generate(currentUserService.require(authentication)), request.getRequestURI());
    }

    @PostMapping("/plans/regenerate")
    public ApiResponse<WorkoutPlanResponse> regenerate(Authentication authentication, HttpServletRequest request) {
        return ApiResponse.success("Weekly workout plan regenerated",
                workoutPlanService.regenerate(currentUserService.require(authentication)), request.getRequestURI());
    }

    @GetMapping("/today")
    public ApiResponse<TodayWorkoutResponse> today(Authentication authentication, HttpServletRequest request) {
        return ApiResponse.success("Today's workout retrieved",
                workoutPlanService.today(currentUserService.require(authentication)), request.getRequestURI());
    }
}
