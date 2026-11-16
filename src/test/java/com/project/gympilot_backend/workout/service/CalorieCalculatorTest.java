package com.project.gympilot_backend.workout.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.project.gympilot_backend.workout.model.Exercise;
import com.project.gympilot_backend.workout.model.WorkoutPlanItem;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CalorieCalculatorTest {
    @Test
    void estimatesCaloriesFromMetWeightAndCompletedWork() {
        Exercise exercise = Exercise.builder()
                .name("Bodyweight Squat")
                .metValue(BigDecimal.valueOf(5))
                .build();
        WorkoutPlanItem item = WorkoutPlanItem.builder()
                .exercise(exercise)
                .sets(3)
                .targetRepetitions(10)
                .build();

        BigDecimal estimate = CalorieCalculator.estimatePlanItem(
                item, BigDecimal.valueOf(80));

        assertThat(estimate).isEqualByComparingTo("10.50");
    }

    @Test
    void skippedExerciseEarnsNoCalories() {
        Exercise exercise = Exercise.builder()
                .metValue(BigDecimal.valueOf(8))
                .build();
        WorkoutPlanItem item = WorkoutPlanItem.builder()
                .exercise(exercise)
                .sets(4)
                .targetRepetitions(12)
                .build();

        assertThat(CalorieCalculator.estimateCompleted(
                item, BigDecimal.valueOf(70), 4, 12, true))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }
}
