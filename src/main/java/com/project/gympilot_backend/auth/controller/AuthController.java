package com.project.gympilot_backend.auth.controller;

import com.project.gympilot_backend.auth.dto.AuthResponse;
import com.project.gympilot_backend.auth.dto.ChangePasswordRequest;
import com.project.gympilot_backend.auth.dto.ForgotPasswordRequest;
import com.project.gympilot_backend.auth.dto.LoginRequest;
import com.project.gympilot_backend.auth.dto.RefreshTokenRequest;
import com.project.gympilot_backend.auth.dto.RegisterRequest;
import com.project.gympilot_backend.auth.dto.RegistrationResponse;
import com.project.gympilot_backend.auth.dto.ResendVerificationRequest;
import com.project.gympilot_backend.auth.dto.ResetPasswordRequest;
import com.project.gympilot_backend.auth.dto.TokenDeliveryResponse;
import com.project.gympilot_backend.auth.dto.VerifyEmailRequest;
import com.project.gympilot_backend.auth.service.AuthService;
import com.project.gympilot_backend.auth.service.EmailVerificationPageRenderer;
import com.project.gympilot_backend.common.exception.ApiException;
import com.project.gympilot_backend.common.response.ApiResponse;
import com.project.gympilot_backend.security.service.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final CurrentUserService currentUserService;
    private final EmailVerificationPageRenderer emailVerificationPageRenderer;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegistrationResponse>> register(
            @Valid @RequestBody RegisterRequest body, HttpServletRequest request) {
        RegistrationResponse response = authService.register(body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account created; verify your email before signing in",
                        response, request.getRequestURI()));
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

    @GetMapping(value = "/verify-email-link", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> verifyEmailLink(
            @RequestParam(name = "token", required = false) String token) {
        if (token == null || token.isBlank()) {
            return verificationPageResponse(HttpStatus.BAD_REQUEST, emailVerificationPageRenderer.failure());
        }
        try {
            authService.verifyEmail(token);
            return verificationPageResponse(HttpStatus.OK, emailVerificationPageRenderer.success());
        } catch (ApiException exception) {
            return verificationPageResponse(exception.getStatus(), emailVerificationPageRenderer.failure());
        }
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest body,
            HttpServletRequest request) {
        authService.changePassword(currentUserService.require(authentication), body);
        return ApiResponse.success("Password changed; sign in again on other devices", null, request.getRequestURI());
    }

    @GetMapping("/me")
    public ApiResponse<com.project.gympilot_backend.auth.dto.AuthUserResponse> me(
            Authentication authentication, HttpServletRequest request) {
        var user = currentUserService.require(authentication);
        return ApiResponse.success("Authenticated user retrieved", authService.currentUser(user), request.getRequestURI());
    }

    private ResponseEntity<String> verificationPageResponse(HttpStatus status, String page) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);
        headers.setCacheControl("no-store, max-age=0");
        headers.set("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'");
        headers.set("Referrer-Policy", "no-referrer");
        headers.set("X-Frame-Options", "DENY");
        headers.set("X-Content-Type-Options", "nosniff");
        return ResponseEntity.status(status).headers(headers).body(page);
    }
}
