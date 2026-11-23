package com.project.gympilot_backend.wellness.sleep.dto;

import com.project.gympilot_backend.wellness.sleep.model.SleepQuality;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record SleepLogRequest(
        @NotNull Instant sleptAt,
        @NotNull Instant wokeAt,
        @NotNull SleepQuality quality,
        @NotNull @Min(1) @Max(5) Integer energyLevel,
        @Size(max = 500) String notes
) {
}
