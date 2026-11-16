package com.project.gympilot_backend.vision.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record VisionWorkoutSummaryRequest(
        @NotNull UUID workoutSessionId,
        @NotNull UUID exerciseId,
        @NotNull @Min(1) @Max(20) Integer completedSets,
        @NotNull @Min(0) @Max(100) Integer completedRepetitions,
        @DecimalMin("0.10") @DecimalMax("30.00") BigDecimal averageTempoSeconds,
        @Size(max = 10) List<@Size(max = 80) String> formFlags,
        @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal score
) {
}
