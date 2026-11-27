package com.project.gympilot_backend.coach.dto;

import com.project.gympilot_backend.coach.model.BriefType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CoachContext(
        BriefType briefType,
        LocalDate fromDate,
        LocalDate toDate,
        ProfileFacts profile,
        TodayWorkoutFacts todayWorkout,
        WorkoutFacts workouts,
        WellnessFacts wellness,
        List<String> missingData,
        List<String> evidence
) {
    public record ProfileFacts(
            String fitnessGoal,
            String experienceLevel,
            int plannedTrainingDays,
            int preferredSessionMinutes
    ) {
    }

    public record TodayWorkoutFacts(
            boolean planAvailable,
            boolean restDay,
            String title,
            Integer estimatedMinutes,
            Integer exerciseCount
    ) {
    }

    public record WorkoutFacts(
            int recordedSessions,
            int completedWorkouts,
            int skippedWorkouts,
            int completedExercises,
            BigDecimal caloriesBurned,
            Double averageFormScore,
            List<String> formFlags
    ) {
    }

    public record WellnessFacts(
            long averageSleepMinutes,
            Integer sleepTargetMinutes,
            double averageEnergyLevel,
            int hydrationGoalDays,
            int periodDays,
            int waterTodayMl,
            int waterTargetMl,
            int nutritionCalories,
            BigDecimal proteinGrams,
            BigDecimal carbohydrateGrams,
            BigDecimal fatGrams
    ) {
    }
}
