package com.project.gympilot_backend.notification.controller;

import com.project.gympilot_backend.common.response.ApiResponse;
import com.project.gympilot_backend.notification.dto.NotificationPreferenceRequest;
import com.project.gympilot_backend.notification.dto.NotificationPreferenceResponse;
import com.project.gympilot_backend.notification.service.NotificationPreferenceService;
import com.project.gympilot_backend.security.service.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications/preferences")
public class NotificationPreferenceController {
    private final CurrentUserService currentUserService;
    private final NotificationPreferenceService service;

    public NotificationPreferenceController(
            CurrentUserService currentUserService,
            NotificationPreferenceService service
    ) {
        this.currentUserService = currentUserService;
        this.service = service;
    }

    @GetMapping
    public ApiResponse<NotificationPreferenceResponse> get(
            Authentication authentication,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                "Notification preferences retrieved",
                service.get(currentUserService.require(authentication)),
                request.getRequestURI());
    }

    @PutMapping
    public ApiResponse<NotificationPreferenceResponse> update(
            Authentication authentication,
            @Valid @RequestBody NotificationPreferenceRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                "Notification preferences updated",
                service.update(currentUserService.require(authentication), body),
                request.getRequestURI());
    }
}
