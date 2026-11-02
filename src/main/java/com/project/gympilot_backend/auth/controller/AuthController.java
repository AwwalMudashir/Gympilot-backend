package com.project.gympilot_backend.auth.controller;

import com.project.gympilot_backend.auth.dto.AuthResponse;
import com.project.gympilot_backend.auth.dto.ChangePasswordRequest;
import com.project.gympilot_backend.auth.dto.ForgotPasswordRequest;
import com.project.gympilot_backend.auth.dto.LoginRequest;
import com.project.gympilot_backend.auth.dto.RefreshTokenRequest;
import com.project.gympilot_backend.auth.dto.RegisterRequest;
import com.project.gympilot_backend.auth.dto.ResendVerificationRequest;
import com.project.gympilot_backend.auth.dto.ResetPasswordRequest;
import com.project.gympilot_backend.auth.dto.TokenDeliveryResponse;
import com.project.gympilot_backend.auth.dto.VerifyEmailRequest;
import com.project.gympilot_backend.auth.service.AuthService;
import com.project.gympilot_backend.common.response.ApiResponse;
import com.project.gympilot_backend.security.service.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final CurrentUserService currentUserService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest body, HttpServletRequest request) {
        AuthResponse response = authService.register(body, request.getRemoteAddr(), request.getHeader("User-Agent"));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account created", response, request.getRequestURI()));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest body, HttpServletRequest request) {
        return ApiResponse.success("Login successful",
                authService.login(body, request.getRemoteAddr(), request.getHeader("User-Agent")),
                request.getRequestURI());
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest body, HttpServletRequest request) {
        return ApiResponse.success("Tokens refreshed",
                authService.refresh(body.refreshToken(), request.getRemoteAddr(), request.getHeader("User-Agent")),
                request.getRequestURI());
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest body, HttpServletRequest request) {
        authService.logout(body.refreshToken());
        return ApiResponse.success("Logged out", null, request.getRequestURI());
    }

    @PostMapping("/forgot-password")
    public ApiResponse<TokenDeliveryResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest body, HttpServletRequest request) {
        return ApiResponse.success("If the account exists, password reset instructions have been sent",
                authService.forgotPassword(body.email()), request.getRequestURI());
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest body, HttpServletRequest request) {
        authService.resetPassword(body);
        return ApiResponse.success("Password reset successful", null, request.getRequestURI());
    }

    @PostMapping("/resend-verification")
    public ApiResponse<TokenDeliveryResponse> resendVerification(
            @Valid @RequestBody ResendVerificationRequest body, HttpServletRequest request) {
        return ApiResponse.success("If verification is required, a new verification email has been sent",
                authService.resendVerification(body.email()), request.getRequestURI());
    }

    @PostMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest body, HttpServletRequest request) {
        authService.verifyEmail(body.token());
        return ApiResponse.success("Email verified", null, request.getRequestURI());
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest body,
            HttpServletRequest request) {
        authService.changePassword(currentUserService.require(authentication), body);
        return ApiResponse.success("Password changed; sign in again on other devices", null, request.getRequestURI());
    }
}
