package com.project.gympilot_backend.admin.controller;

import com.project.gympilot_backend.admin.dto.AdminUserResponse;
import com.project.gympilot_backend.admin.service.AdminUserService;
import com.project.gympilot_backend.common.response.ApiResponse;
import com.project.gympilot_backend.security.service.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    private final CurrentUserService currentUserService;
    private final AdminUserService adminUserService;

    public AdminUserController(CurrentUserService currentUserService, AdminUserService adminUserService) {
        this.currentUserService = currentUserService;
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ApiResponse<List<AdminUserResponse>> list(HttpServletRequest request) {
        return ApiResponse.success("Users retrieved", adminUserService.listUsers(), request.getRequestURI());
    }

    @PatchMapping("/{userId}/deactivate")
    public ApiResponse<AdminUserResponse> deactivate(
            Authentication authentication,
            @PathVariable UUID userId,
            HttpServletRequest request
    ) {
        var administrator = currentUserService.require(authentication);
        return ApiResponse.success(
                "Account deactivated",
                adminUserService.deactivate(administrator, userId),
                request.getRequestURI());
    }

    @PatchMapping("/{userId}/reactivate")
    public ApiResponse<AdminUserResponse> reactivate(
            Authentication authentication,
            @PathVariable UUID userId,
            HttpServletRequest request
    ) {
        var administrator = currentUserService.require(authentication);
        return ApiResponse.success(
                "Account reactivated",
                adminUserService.reactivate(administrator, userId),
                request.getRequestURI());
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> permanentlyDelete(
            Authentication authentication,
            @PathVariable UUID userId,
            @RequestParam String confirmEmail,
            HttpServletRequest request
    ) {
        var administrator = currentUserService.require(authentication);
        adminUserService.permanentlyDelete(administrator, userId, confirmEmail);
        return ResponseEntity.ok(ApiResponse.success(
                "Account and related data permanently deleted",
                null,
                request.getRequestURI()));
    }
}
