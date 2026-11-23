package com.project.gympilot_backend.wellness.sleep.controller;

import com.project.gympilot_backend.common.response.ApiResponse;
import com.project.gympilot_backend.security.service.CurrentUserService;
import com.project.gympilot_backend.wellness.sleep.dto.SleepLogRequest;
import com.project.gympilot_backend.wellness.sleep.dto.SleepLogResponse;
import com.project.gympilot_backend.wellness.sleep.service.SleepLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wellness/sleep")
public class SleepLogController {
    private final CurrentUserService currentUserService;
    private final SleepLogService service;

    public SleepLogController(CurrentUserService currentUserService, SleepLogService service) {
        this.currentUserService = currentUserService;
        this.service = service;
    }

    @PostMapping
    public ApiResponse<SleepLogResponse> create(
            Authentication authentication,
            @Valid @RequestBody SleepLogRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                "Sleep check-in saved",
                service.create(currentUserService.require(authentication), body),
                request.getRequestURI());
    }

    @GetMapping
    public ApiResponse<List<SleepLogResponse>> list(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                "Sleep logs retrieved",
                service.list(currentUserService.require(authentication), fromDate, toDate),
                request.getRequestURI());
    }
}
