package com.project.gympilot_backend.media.service;

public record CloudinaryUploadResult(
        String assetId,
        String publicId,
        String secureUrl,
        String resourceType,
        String format,
        long byteSize,
        Integer width,
        Integer height
) {}
