package com.project.gympilot_backend.wellness.progress.controller;

import com.project.gympilot_backend.common.response.ApiResponse;
import com.project.gympilot_backend.security.service.CurrentUserService;
import com.project.gympilot_backend.wellness.progress.dto.WellnessProgressResponse;
import com.project.gympilot_backend.wellness.progress.service.WellnessProgressService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/wellness/progress")
public class WellnessProgressController {
    private final CurrentUserService currentUserService;
    private final WellnessProgressService service;

    public WellnessProgressController(
            CurrentUserService currentUserService,
            WellnessProgressService service
    ) {
        this.currentUserService = currentUserService;
        this.service = service;
    }

    @GetMapping
    public ApiResponse<WellnessProgressResponse> progress(
            Authentication authentication,
            @RequestParam(defaultValue = "7") @Min(1) @Max(31) int days,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                "Wellness progress retrieved",
                service.progress(currentUserService.require(authentication), days),
                request.getRequestURI());
    }
}
