package com.project.gympilot_backend.media.service;

import com.project.gympilot_backend.common.exception.BadRequestException;
import com.project.gympilot_backend.config.CloudinaryProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class ImageFileValidator {
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");

    private final CloudinaryProperties properties;

    public byte[] validateAndRead(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("IMAGE_REQUIRED", "An image file is required");
        }
        if (file.getSize() > properties.getMaxImageBytes()) {
            throw new BadRequestException("IMAGE_TOO_LARGE", "Image exceeds the configured upload limit");
        }
        String contentType = file.getContentType() == null
                ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("IMAGE_TYPE_NOT_ALLOWED", "Only JPEG, PNG, WebP and GIF images are allowed");
        }
        try {
            byte[] content = file.getBytes();
            if (!hasSupportedSignature(content)) {
                throw new BadRequestException("IMAGE_CONTENT_INVALID", "File content is not a supported image");
            }
            return content;
        } catch (IOException exception) {
            throw new BadRequestException("IMAGE_READ_FAILED", "Image could not be read");
        }
    }

    private boolean hasSupportedSignature(byte[] bytes) {
        return isJpeg(bytes) || isPng(bytes) || isWebp(bytes) || isGif(bytes);
    }

    private boolean isJpeg(byte[] bytes) {
        return bytes.length >= 3 && unsigned(bytes[0]) == 0xFF && unsigned(bytes[1]) == 0xD8
                && unsigned(bytes[2]) == 0xFF;
    }

    private boolean isPng(byte[] bytes) {
        int[] signature = {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        if (bytes.length < signature.length) return false;
        for (int i = 0; i < signature.length; i++) {
            if (unsigned(bytes[i]) != signature[i]) return false;
        }
        return true;
    }

    private boolean isWebp(byte[] bytes) {
        return bytes.length >= 12
                && ascii(bytes, 0, 4).equals("RIFF")
                && ascii(bytes, 8, 4).equals("WEBP");
    }

    private boolean isGif(byte[] bytes) {
        if (bytes.length < 6) return false;
        String header = ascii(bytes, 0, 6);
        return header.equals("GIF87a") || header.equals("GIF89a");
    }

    private String ascii(byte[] bytes, int start, int length) {
        return new String(bytes, start, length, StandardCharsets.US_ASCII);
    }

    private int unsigned(byte value) {
        return value & 0xFF;
    }
}
