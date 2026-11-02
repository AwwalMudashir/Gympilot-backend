package com.project.gympilot_backend.user.controller;

import com.project.gympilot_backend.common.response.ApiResponse;
import com.project.gympilot_backend.security.service.CurrentUserService;
import com.project.gympilot_backend.user.dto.UpdateAccountRequest;
import com.project.gympilot_backend.user.dto.UserAccountResponse;
import com.project.gympilot_backend.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final CurrentUserService currentUserService;
    private final UserService userService;

    public UserController(CurrentUserService currentUserService, UserService userService) {
        this.currentUserService = currentUserService;
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserAccountResponse> getMe(Authentication authentication, HttpServletRequest request) {
        var data = userService.getAccount(currentUserService.require(authentication));
        return ApiResponse.success("Account retrieved", data, request.getRequestURI());
    }

    @PatchMapping("/me")
    public ApiResponse<UserAccountResponse> updateMe(
            Authentication authentication,
            @Valid @RequestBody UpdateAccountRequest body,
            HttpServletRequest request
    ) {
        var data = userService.updateAccount(currentUserService.require(authentication), body);
        return ApiResponse.success("Account updated", data, request.getRequestURI());
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            Authentication authentication,
            HttpServletRequest request
    ) {
        userService.deactivate(currentUserService.require(authentication));
        return ResponseEntity.ok(ApiResponse.success("Account deactivated", null, request.getRequestURI()));
    }
}
