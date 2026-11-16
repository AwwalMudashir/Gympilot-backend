package com.project.gympilot_backend.workout.service;

import com.project.gympilot_backend.workout.model.Exercise;
import com.project.gympilot_backend.workout.model.WorkoutPlanItem;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class CalorieCalculator {
    private static final BigDecimal DEFAULT_WEIGHT_KG = BigDecimal.valueOf(70);
    private static final BigDecimal DEFAULT_MET = BigDecimal.valueOf(4);
    private static final BigDecimal MET_CONSTANT = BigDecimal.valueOf(3.5);
    private static final BigDecimal MINUTES_DIVISOR = BigDecimal.valueOf(200);

    private CalorieCalculator() {
    }

    public static BigDecimal estimatePlanItem(WorkoutPlanItem item, BigDecimal weightKg) {
        int activeSeconds = item.getSets()
                * item.getTargetRepetitions()
                * WorkoutTimingCalculator.REPETITION_SECONDS;
        return estimate(item.getExercise(), weightKg, activeSeconds);
    }

    public static BigDecimal estimateCompleted(
            WorkoutPlanItem item,
            BigDecimal weightKg,
            int completedSets,
            int completedRepetitions,
            boolean skipped
    ) {
        if (skipped || completedSets <= 0 || completedRepetitions <= 0) return BigDecimal.ZERO;
        int activeSeconds = completedSets
                * completedRepetitions
                * WorkoutTimingCalculator.REPETITION_SECONDS;
        return estimate(item.getExercise(), weightKg, activeSeconds);
    }

    public static BigDecimal estimate(Exercise exercise, BigDecimal weightKg, int activeSeconds) {
        if (activeSeconds <= 0) return BigDecimal.ZERO;
        BigDecimal safeWeight = weightKg == null || weightKg.signum() <= 0
                ? DEFAULT_WEIGHT_KG : weightKg;
        BigDecimal met = exercise.getMetValue() == null || exercise.getMetValue().signum() <= 0
                ? DEFAULT_MET : exercise.getMetValue();
        BigDecimal minutes = BigDecimal.valueOf(activeSeconds)
                .divide(BigDecimal.valueOf(60), 6, RoundingMode.HALF_UP);
        return met.multiply(MET_CONSTANT)
                .multiply(safeWeight)
                .divide(MINUTES_DIVISOR, 6, RoundingMode.HALF_UP)
                .multiply(minutes)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
