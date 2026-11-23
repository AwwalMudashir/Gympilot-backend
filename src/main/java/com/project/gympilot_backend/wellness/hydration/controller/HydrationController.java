package com.project.gympilot_backend.wellness.hydration.controller;

import com.project.gympilot_backend.common.response.ApiResponse;
import com.project.gympilot_backend.security.service.CurrentUserService;
import com.project.gympilot_backend.wellness.hydration.dto.DailyHydrationResponse;
import com.project.gympilot_backend.wellness.hydration.dto.WaterLogRequest;
import com.project.gympilot_backend.wellness.hydration.dto.WaterLogResponse;
import com.project.gympilot_backend.wellness.hydration.service.HydrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wellness/hydration")
public class HydrationController {
    private final CurrentUserService currentUserService;
    private final HydrationService service;

    public HydrationController(CurrentUserService currentUserService, HydrationService service) {
        this.currentUserService = currentUserService;
        this.service = service;
    }

    @PostMapping
    public ApiResponse<WaterLogResponse> create(
            Authentication authentication,
            @Valid @RequestBody WaterLogRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                "Water intake added",
                service.create(currentUserService.require(authentication), body),
                request.getRequestURI());
    }

    @GetMapping("/daily")
    public ApiResponse<DailyHydrationResponse> daily(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                "Daily hydration retrieved",
                service.daily(currentUserService.require(authentication), date),
                request.getRequestURI());
    }
}
