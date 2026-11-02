package com.project.gympilot_backend.profile.controller;

import com.project.gympilot_backend.common.response.ApiResponse;
import com.project.gympilot_backend.media.dto.ImageUploadResponse;
import com.project.gympilot_backend.media.service.ImageStorageService;
import com.project.gympilot_backend.profile.dto.ProfileRequest;
import com.project.gympilot_backend.profile.dto.ProfileResponse;
import com.project.gympilot_backend.profile.service.ProfileService;
import com.project.gympilot_backend.security.service.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final CurrentUserService currentUserService;
    private final ProfileService profileService;
    private final ImageStorageService imageStorageService;

    public ProfileController(
            CurrentUserService currentUserService,
            ProfileService profileService,
            ImageStorageService imageStorageService
    ) {
        this.currentUserService = currentUserService;
        this.profileService = profileService;
        this.imageStorageService = imageStorageService;
    }

    @GetMapping({"", "/me"})
    public ApiResponse<ProfileResponse> get(Authentication authentication, HttpServletRequest request) {
        var data = profileService.get(currentUserService.require(authentication));
        return ApiResponse.success("Profile retrieved", data, request.getRequestURI());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> create(
            Authentication authentication,
            @Valid @RequestBody ProfileRequest body,
            HttpServletRequest request
    ) {
        var data = profileService.create(currentUserService.require(authentication), body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Profile created", data, request.getRequestURI()));
    }

    @PutMapping
    public ApiResponse<ProfileResponse> update(
            Authentication authentication,
            @Valid @RequestBody ProfileRequest body,
            HttpServletRequest request
    ) {
        var data = profileService.update(currentUserService.require(authentication), body);
        return ApiResponse.success("Profile updated", data, request.getRequestURI());
    }

    @PostMapping(value = "/picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadPicture(
            Authentication authentication,
            @RequestPart("file") MultipartFile file,
            HttpServletRequest request
    ) {
        ImageUploadResponse data = imageStorageService.replaceProfilePicture(
                currentUserService.require(authentication), file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Profile picture uploaded", data, request.getRequestURI()));
    }

    @DeleteMapping("/picture")
    public ApiResponse<Void> deletePicture(Authentication authentication, HttpServletRequest request) {
        imageStorageService.deleteProfilePicture(currentUserService.require(authentication));
        return ApiResponse.success("Profile picture deleted", null, request.getRequestURI());
    }
}
