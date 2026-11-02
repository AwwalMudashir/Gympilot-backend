package com.project.gympilot_backend.media.controller;

import com.project.gympilot_backend.common.response.ApiResponse;
import com.project.gympilot_backend.media.dto.ImageUploadResponse;
import com.project.gympilot_backend.media.model.MediaPurpose;
import com.project.gympilot_backend.media.service.ImageStorageService;
import com.project.gympilot_backend.security.service.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/media/images")
@RequiredArgsConstructor
public class MediaController {
    private final CurrentUserService currentUserService;
    private final ImageStorageService imageStorageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImageUploadResponse>> upload(
            Authentication authentication,
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "GENERAL") MediaPurpose purpose,
            HttpServletRequest request
    ) {
        ImageUploadResponse data = imageStorageService.upload(
                currentUserService.require(authentication), file, purpose);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Image uploaded", data, request.getRequestURI()));
    }

    @DeleteMapping("/{mediaId}")
    public ApiResponse<Void> delete(
            Authentication authentication,
            @PathVariable UUID mediaId,
            HttpServletRequest request
    ) {
        imageStorageService.delete(currentUserService.require(authentication), mediaId);
        return ApiResponse.success("Image deleted", null, request.getRequestURI());
    }
}
