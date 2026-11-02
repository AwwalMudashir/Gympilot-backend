package com.project.gympilot_backend.common.response;

public record ApiError(String code, String field, String message) {
}
