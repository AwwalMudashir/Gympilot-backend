package com.project.gympilot_backend.media.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.project.gympilot_backend.common.exception.StorageException;
import com.project.gympilot_backend.config.CloudinaryProperties;
import com.project.gympilot_backend.media.model.MediaPurpose;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CloudinaryGatewayImpl implements CloudinaryGateway {
    private final Cloudinary cloudinary;
    private final CloudinaryProperties properties;

    @Override
    public CloudinaryUploadResult upload(byte[] content, UUID ownerId, MediaPurpose purpose) {
        requireConfigured();
        String folder = normalizedFolder() + "/" + ownerId + "/" + purpose.name().toLowerCase(Locale.ROOT);
        try {
            Map<?, ?> result = cloudinary.uploader().upload(content, ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "image",
                    "unique_filename", true,
                    "overwrite", false,
                    "tags", new String[]{"gympilot", purpose.name().toLowerCase(Locale.ROOT)}
            ));
            String secureUrl = stringValue(result.get("secure_url"));
            if (secureUrl == null || !secureUrl.startsWith("https://")) {
                throw new StorageException("IMAGE_UPLOAD_INVALID_RESPONSE", "Image storage returned an invalid URL");
            }
            return new CloudinaryUploadResult(
                    stringValue(result.get("asset_id")),
                    requiredString(result, "public_id"),
                    secureUrl,
                    result.get("resource_type") == null ? "image" : stringValue(result.get("resource_type")),
                    stringValue(result.get("format")),
                    longValue(result.get("bytes")),
                    integerValue(result.get("width")),
                    integerValue(result.get("height"))
            );
        } catch (IOException exception) {
            throw new StorageException("IMAGE_UPLOAD_FAILED", "Image could not be uploaded");
        }
    }

    @Override
    public void delete(String publicId) {
        requireConfigured();
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", "image",
                    "invalidate", true
            ));
        } catch (IOException exception) {
            throw new StorageException("IMAGE_DELETE_FAILED", "Image could not be deleted");
        }
    }

    private void requireConfigured() {
        if (!properties.isConfigured()) {
            throw StorageException.notConfigured();
        }
    }

    private String normalizedFolder() {
        String folder = properties.getFolder() == null ? "gympilot" : properties.getFolder().trim();
        folder = folder.replaceAll("[^A-Za-z0-9/_-]", "-");
        folder = folder.replaceAll("^/+|/+$", "");
        return folder.isBlank() ? "gympilot" : folder;
    }

    private String requiredString(Map<?, ?> result, String key) {
        String value = stringValue(result.get(key));
        if (value == null || value.isBlank()) {
            throw new StorageException("IMAGE_UPLOAD_INVALID_RESPONSE", "Image storage returned incomplete metadata");
        }
        return value;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private Integer integerValue(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }
}
