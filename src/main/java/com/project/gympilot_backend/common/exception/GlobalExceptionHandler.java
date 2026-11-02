package com.project.gympilot_backend.common.exception;

import com.project.gympilot_backend.common.response.ApiError;
import com.project.gympilot_backend.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(
            ApiException exception,
            HttpServletRequest request
    ) {
        var error = new ApiError(exception.getCode(), null, exception.getMessage());
        return ResponseEntity.status(exception.getStatus())
                .body(ApiResponse.error(exception.getMessage(), List.of(error), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<ApiError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toApiError)
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Validation failed", errors, request.getRequestURI()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        var error = new ApiError("MALFORMED_REQUEST", null, "Request body is missing or malformed");
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid request", List.of(error), request.getRequestURI()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        List<ApiError> errors = exception.getConstraintViolations().stream()
                .map(violation -> new ApiError(
                        "VALIDATION_ERROR", violation.getPropertyPath().toString(), violation.getMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Validation failed", errors, request.getRequestURI()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request
    ) {
        var error = new ApiError("MISSING_PARAMETER", exception.getParameterName(), "Required parameter is missing");
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid request", List.of(error), request.getRequestURI()));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingPart(
            MissingServletRequestPartException exception,
            HttpServletRequest request
    ) {
        var error = new ApiError("MISSING_MULTIPART_PART", exception.getRequestPartName(),
                "Required multipart field is missing");
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid upload request", List.of(error), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        var error = new ApiError("INVALID_PARAMETER", exception.getName(), "Parameter value is invalid");
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid request", List.of(error), request.getRequestURI()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleUploadTooLarge(
            MaxUploadSizeExceededException exception,
            HttpServletRequest request
    ) {
        var error = new ApiError("IMAGE_TOO_LARGE", "file", "Image exceeds the upload limit");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error("Upload too large", List.of(error), request.getRequestURI()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {
        var error = new ApiError("METHOD_NOT_ALLOWED", null, "HTTP method is not supported for this endpoint");
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error("Method not allowed", List.of(error), request.getRequestURI()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {
        var error = new ApiError("ENDPOINT_NOT_FOUND", null, "The requested endpoint does not exist");
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Not found", List.of(error), request.getRequestURI()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        log.warn("Database constraint violation", exception);
        var error = new ApiError("DATA_CONFLICT", null, "The request conflicts with existing data");
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("Data conflict", List.of(error), request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error("Unhandled request error", exception);
        var error = new ApiError("INTERNAL_ERROR", null, "An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Request failed", List.of(error), request.getRequestURI()));
    }

    private ApiError toApiError(FieldError error) {
        return new ApiError("VALIDATION_ERROR", error.getField(), error.getDefaultMessage());
    }
}
