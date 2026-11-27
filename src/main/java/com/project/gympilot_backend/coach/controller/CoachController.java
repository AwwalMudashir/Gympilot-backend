package com.project.gympilot_backend.coach.controller;

import com.project.gympilot_backend.coach.dto.CoachBriefResponse;
import com.project.gympilot_backend.coach.dto.CoachFeedbackRequest;
import com.project.gympilot_backend.coach.service.CoachBriefService;
import com.project.gympilot_backend.common.response.ApiResponse;
import com.project.gympilot_backend.security.service.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coach")
public class CoachController {
    private final CurrentUserService currentUserService;
    private final CoachBriefService coachBriefService;

    public CoachController(
            CurrentUserService currentUserService,
            CoachBriefService coachBriefService
    ) {
        this.currentUserService = currentUserService;
        this.coachBriefService = coachBriefService;
    }

    @GetMapping("/daily")
    public ApiResponse<CoachBriefResponse> daily(
            Authentication authentication,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                "Daily coaching brief retrieved",
                coachBriefService.daily(currentUserService.require(authentication), false),
                request.getRequestURI());
    }

    @PostMapping("/daily/regenerate")
    public ApiResponse<CoachBriefResponse> regenerateDaily(
            Authentication authentication,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                "Daily coaching brief regenerated",
                coachBriefService.daily(currentUserService.require(authentication), true),
                request.getRequestURI());
    }

    @GetMapping("/weekly")
    public ApiResponse<CoachBriefResponse> weekly(
            Authentication authentication,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                "Weekly coaching brief retrieved",
                coachBriefService.weekly(currentUserService.require(authentication), false),
                request.getRequestURI());
    }

    @PostMapping("/weekly/regenerate")
    public ApiResponse<CoachBriefResponse> regenerateWeekly(
            Authentication authentication,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                "Weekly coaching brief regenerated",
                coachBriefService.weekly(currentUserService.require(authentication), true),
                request.getRequestURI());
    }

    @PutMapping("/briefs/{briefId}/feedback")
    public ApiResponse<CoachBriefResponse> recordFeedback(
            Authentication authentication,
            @PathVariable UUID briefId,
            @Valid @RequestBody CoachFeedbackRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                "Coaching feedback saved",
                coachBriefService.recordFeedback(
                        currentUserService.require(authentication), briefId, body.feedback()),
                request.getRequestURI());
    }
}
