package com.project.gympilot_backend.vision.controller;

import com.project.gympilot_backend.common.response.ApiResponse;
import com.project.gympilot_backend.security.service.CurrentUserService;
import com.project.gympilot_backend.vision.dto.VisionWorkoutSummaryRequest;
import com.project.gympilot_backend.vision.service.VisionWorkoutResultService;
import com.project.gympilot_backend.workout.dto.ExerciseResultResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vision/workout-results")
public class VisionWorkoutResultController {
    private final CurrentUserService currentUserService;
    private final VisionWorkoutResultService resultService;

    public VisionWorkoutResultController(
            CurrentUserService currentUserService,
            VisionWorkoutResultService resultService
    ) {
        this.currentUserService = currentUserService;
        this.resultService = resultService;
    }

    @PostMapping
    public ApiResponse<ExerciseResultResponse> save(
            Authentication authentication,
            @Valid @RequestBody VisionWorkoutSummaryRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                "Vision workout result saved",
                resultService.save(currentUserService.require(authentication), body),
                request.getRequestURI());
    }
}
