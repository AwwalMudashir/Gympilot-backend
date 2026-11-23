package com.project.gympilot_backend.wellness.nutrition.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record NutritionLogRequest(
        @NotBlank @Size(max = 160) String mealName,
        @NotNull @Min(0) @Max(10000) Integer calories,
        @NotNull @DecimalMin("0.00") @DecimalMax("1000.00") BigDecimal proteinGrams,
        @NotNull @DecimalMin("0.00") @DecimalMax("1000.00") BigDecimal carbohydrateGrams,
        @NotNull @DecimalMin("0.00") @DecimalMax("1000.00") BigDecimal fatGrams,
        @DecimalMin("0.00") @DecimalMax("200.00") BigDecimal fibreGrams,
        Instant loggedAt
) {
}
