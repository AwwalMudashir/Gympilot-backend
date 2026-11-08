package com.project.gympilot_backend.workout.controller;

import com.project.gympilot_backend.common.response.ApiResponse;
import com.project.gympilot_backend.security.service.CurrentUserService;
import com.project.gympilot_backend.workout.dto.CompleteWorkoutSessionRequest;
import com.project.gympilot_backend.workout.dto.CreateWorkoutSessionRequest;
import com.project.gympilot_backend.workout.dto.RescheduleWorkoutSessionRequest;
import com.project.gympilot_backend.workout.dto.SkipWorkoutSessionRequest;
import com.project.gympilot_backend.workout.dto.WorkoutSessionResponse;
import com.project.gympilot_backend.workout.service.WorkoutSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workouts/sessions")
public class WorkoutSessionController {
    private final CurrentUserService currentUserService;
    private final WorkoutSessionService sessionService;

    public WorkoutSessionController(CurrentUserService currentUserService, WorkoutSessionService sessionService) {
        this.currentUserService = currentUserService;
        this.sessionService = sessionService;
    }

    @PostMapping
    public ApiResponse<WorkoutSessionResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreateWorkoutSessionRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success("Workout session ready",
                sessionService.createOrGet(currentUserService.require(authentication), body), request.getRequestURI());
    }

    @PostMapping("/{sessionId}/start")
    public ApiResponse<WorkoutSessionResponse> start(
            Authentication authentication, @PathVariable UUID sessionId, HttpServletRequest request
    ) {
        return ApiResponse.success("Workout started",
                sessionService.start(currentUserService.require(authentication), sessionId), request.getRequestURI());
    }

    @PatchMapping("/{sessionId}/complete")
    public ApiResponse<WorkoutSessionResponse> complete(
            Authentication authentication,
            @PathVariable UUID sessionId,
            @Valid @RequestBody CompleteWorkoutSessionRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success("Workout completed",
                sessionService.complete(currentUserService.require(authentication), sessionId, body),
                request.getRequestURI());
    }

    @PatchMapping("/{sessionId}/skip")
    public ApiResponse<WorkoutSessionResponse> skip(
            Authentication authentication,
            @PathVariable UUID sessionId,
            @Valid @RequestBody SkipWorkoutSessionRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success("Workout skipped",
                sessionService.skip(currentUserService.require(authentication), sessionId, body),
                request.getRequestURI());
    }

    @PatchMapping("/{sessionId}/reschedule")
    public ApiResponse<WorkoutSessionResponse> reschedule(
            Authentication authentication,
            @PathVariable UUID sessionId,
            @Valid @RequestBody RescheduleWorkoutSessionRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success("Workout rescheduled",
                sessionService.reschedule(currentUserService.require(authentication), sessionId, body),
                request.getRequestURI());
    }

    @GetMapping("/history")
    public ApiResponse<List<WorkoutSessionResponse>> history(
            Authentication authentication, HttpServletRequest request
    ) {
        return ApiResponse.success("Workout history retrieved",
                sessionService.history(currentUserService.require(authentication)), request.getRequestURI());
    }
}
