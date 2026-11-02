package com.project.gympilot_backend.media.dto;

import com.project.gympilot_backend.media.model.MediaAsset;
import com.project.gympilot_backend.media.model.MediaPurpose;
import java.time.Instant;
import java.util.UUID;

public record ImageUploadResponse(
        UUID id,
        MediaPurpose purpose,
        String url,
        String format,
        long byteSize,
        Integer width,
        Integer height,
        Instant uploadedAt
) {
    public static ImageUploadResponse from(MediaAsset asset) {
        return new ImageUploadResponse(asset.getId(), asset.getPurpose(), asset.getSecureUrl(), asset.getFormat(),
                asset.getByteSize(), asset.getWidth(), asset.getHeight(), asset.getCreatedAt());
    }
}
