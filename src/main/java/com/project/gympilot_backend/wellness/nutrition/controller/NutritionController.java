package com.project.gympilot_backend.wellness.nutrition.controller;

import com.project.gympilot_backend.common.response.ApiResponse;
import com.project.gympilot_backend.security.service.CurrentUserService;
import com.project.gympilot_backend.wellness.nutrition.dto.DailyNutritionResponse;
import com.project.gympilot_backend.wellness.nutrition.dto.NutritionLogRequest;
import com.project.gympilot_backend.wellness.nutrition.dto.NutritionLogResponse;
import com.project.gympilot_backend.wellness.nutrition.service.NutritionService;
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
@RequestMapping("/api/wellness/nutrition")
public class NutritionController {
    private final CurrentUserService currentUserService;
    private final NutritionService service;

    public NutritionController(CurrentUserService currentUserService, NutritionService service) {
        this.currentUserService = currentUserService;
        this.service = service;
    }

    @PostMapping
    public ApiResponse<NutritionLogResponse> create(
            Authentication authentication,
            @Valid @RequestBody NutritionLogRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                "Nutrition entry saved",
                service.create(currentUserService.require(authentication), body),
                request.getRequestURI());
    }

    @GetMapping("/daily")
    public ApiResponse<DailyNutritionResponse> daily(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                "Daily nutrition retrieved",
                service.daily(currentUserService.require(authentication), date),
                request.getRequestURI());
    }
}
