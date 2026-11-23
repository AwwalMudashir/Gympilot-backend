package com.project.gympilot_backend.wellness.nutrition.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DailyNutritionResponse(
        LocalDate date,
        int calories,
        BigDecimal proteinGrams,
        BigDecimal carbohydrateGrams,
        BigDecimal fatGrams,
        BigDecimal fibreGrams,
        List<NutritionLogResponse> entries
) {
}
