package com.project.gympilot_backend.common.exception;

import org.springframework.http.HttpStatus;

public class StorageException extends ApiException {
    public StorageException(String code, String message) {
        super(HttpStatus.BAD_GATEWAY, code, message);
    }

    public static StorageException notConfigured() {
        return new StorageException("IMAGE_STORAGE_NOT_CONFIGURED", "Image storage is not configured");
    }
}
