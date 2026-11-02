package com.project.gympilot_backend.media.service;

import com.project.gympilot_backend.media.model.MediaPurpose;
import java.util.UUID;

public interface CloudinaryGateway {
    CloudinaryUploadResult upload(byte[] content, UUID ownerId, MediaPurpose purpose);

    void delete(String publicId);
}
