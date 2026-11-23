package com.project.gympilot_backend.wellness.nutrition.dto;

import com.project.gympilot_backend.wellness.nutrition.model.NutritionLog;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record NutritionLogResponse(
        UUID id,
        String mealName,
        int calories,
        BigDecimal proteinGrams,
        BigDecimal carbohydrateGrams,
        BigDecimal fatGrams,
        BigDecimal fibreGrams,
        Instant loggedAt
) {
    public static NutritionLogResponse from(NutritionLog log) {
        return new NutritionLogResponse(
                log.getId(), log.getMealName(), log.getCalories(), log.getProteinGrams(),
                log.getCarbohydrateGrams(), log.getFatGrams(), log.getFibreGrams(),
                log.getLoggedAt());
    }
}
