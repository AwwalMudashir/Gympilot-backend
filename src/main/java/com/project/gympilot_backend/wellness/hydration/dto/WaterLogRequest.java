package com.project.gympilot_backend.wellness.hydration.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record WaterLogRequest(
        @NotNull @Min(50) @Max(5000) Integer amountMl,
        Instant loggedAt
) {
}
