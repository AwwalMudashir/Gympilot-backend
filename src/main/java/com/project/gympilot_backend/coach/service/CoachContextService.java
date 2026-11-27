package com.project.gympilot_backend.coach.service;

import com.project.gympilot_backend.coach.dto.CoachContext;
import com.project.gympilot_backend.coach.model.BriefType;
import com.project.gympilot_backend.common.exception.ResourceNotFoundException;
import com.project.gympilot_backend.profile.model.UserProfile;
import com.project.gympilot_backend.profile.repository.UserProfileRepository;
import com.project.gympilot_backend.user.model.User;
import com.project.gympilot_backend.vision.model.ExerciseResult;
import com.project.gympilot_backend.vision.repository.ExerciseResultRepository;
import com.project.gympilot_backend.wellness.progress.dto.WellnessDayResponse;
import com.project.gympilot_backend.wellness.progress.dto.WellnessProgressResponse;
import com.project.gympilot_backend.wellness.progress.service.WellnessProgressService;
import com.project.gympilot_backend.workout.dto.TodayWorkoutResponse;
import com.project.gympilot_backend.workout.model.SessionStatus;
import com.project.gympilot_backend.workout.model.WorkoutSession;
import com.project.gympilot_backend.workout.repository.WorkoutSessionRepository;
import com.project.gympilot_backend.workout.service.WorkoutPlanService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CoachContextService {
    private final UserProfileRepository profileRepository;
    private final WorkoutSessionRepository sessionRepository;
    private final ExerciseResultRepository resultRepository;
    private final WorkoutPlanService workoutPlanService;
    private final WellnessProgressService wellnessProgressService;
    private final ZoneId applicationZone;

    public CoachContextService(
            UserProfileRepository profileRepository,
            WorkoutSessionRepository sessionRepository,
            ExerciseResultRepository resultRepository,
            WorkoutPlanService workoutPlanService,
            WellnessProgressService wellnessProgressService,
            @Value("${app.time-zone:Africa/Lagos}") String timeZone
    ) {
        this.profileRepository = profileRepository;
        this.sessionRepository = sessionRepository;
        this.resultRepository = resultRepository;
        this.workoutPlanService = workoutPlanService;
        this.wellnessProgressService = wellnessProgressService;
        this.applicationZone = ZoneId.of(timeZone);
    }

    @Transactional(readOnly = true)
    public CoachContext build(User user, BriefType briefType) {
        UserProfile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Complete your fitness profile before opening the AI Coach"));
        if (profile.getFitnessGoal() == null || profile.getExperienceLevel() == null) {
            throw new ResourceNotFoundException(
                    "Complete your fitness profile before opening the AI Coach");
        }
        LocalDate today = LocalDate.now(applicationZone);
        LocalDate from = briefType == BriefType.DAILY
                ? today
                : today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        int periodDays = (int) (today.toEpochDay() - from.toEpochDay()) + 1;

        List<WorkoutSession> sessions = sessionRepository
                .findAllByUserIdAndScheduledDateBetweenOrderByScheduledDateAscCreatedAtAsc(
                        user.getId(), from, today);
        List<UUID> sessionIds = sessions.stream().map(WorkoutSession::getId).toList();
        List<ExerciseResult> results = sessionIds.isEmpty()
                ? List.of()
                : resultRepository.findAllByWorkoutSessionIdIn(sessionIds);
        WellnessProgressResponse wellness = wellnessProgressService.progress(user, periodDays);
        TodayWorkoutResponse todayWorkout = findTodayWorkout(user);

        CoachContext.WorkoutFacts workoutFacts = workoutFacts(sessions, results);
        CoachContext.WellnessFacts wellnessFacts = wellnessFacts(profile, wellness, periodDays);
        List<String> missingData = missingData(todayWorkout, sessions, results, wellnessFacts);
        List<String> evidence = evidence(profile, todayWorkout, workoutFacts, wellnessFacts);

        return new CoachContext(
                briefType,
                from,
                today,
                new CoachContext.ProfileFacts(
                        label(profile.getFitnessGoal().name()),
                        label(profile.getExperienceLevel().name()),
                        valueOrDefault(profile.getDaysPerWeek(), 3),
                        valueOrDefault(profile.getWorkoutDurationMinutes(), 30)),
                todayWorkoutFacts(todayWorkout),
                workoutFacts,
                wellnessFacts,
                List.copyOf(missingData),
                List.copyOf(evidence));
    }

    private TodayWorkoutResponse findTodayWorkout(User user) {
        try {
            return workoutPlanService.today(user);
        } catch (ResourceNotFoundException ignored) {
            return null;
        }
    }

    private CoachContext.TodayWorkoutFacts todayWorkoutFacts(TodayWorkoutResponse today) {
        if (today == null) {
            return new CoachContext.TodayWorkoutFacts(false, false, null, null, null);
        }
        if (today.restDay() || today.workout() == null) {
            return new CoachContext.TodayWorkoutFacts(true, true, "Rest and recovery", null, 0);
        }
        return new CoachContext.TodayWorkoutFacts(
                true,
                false,
                today.workout().title(),
                today.workout().estimatedMinutes(),
                today.workout().exercises().size());
    }

    private CoachContext.WorkoutFacts workoutFacts(
            List<WorkoutSession> sessions,
            List<ExerciseResult> results
    ) {
        List<ExerciseResult> completedResults = results.stream()
                .filter(result -> !result.isSkipped())
                .filter(result -> valueOrDefault(result.getCompletedSets(), 0) > 0)
                .toList();
        List<ExerciseResult> scoredResults = completedResults.stream()
                .filter(result -> result.getFormScore() != null)
                .toList();
        Double averageFormScore = scoredResults.isEmpty()
                ? null
                : BigDecimal.valueOf(scoredResults.stream()
                        .mapToDouble(result -> result.getFormScore().doubleValue())
                        .average()
                        .orElse(0))
                        .setScale(1, RoundingMode.HALF_UP)
                        .doubleValue();
        LinkedHashSet<String> formFlags = new LinkedHashSet<>();
        completedResults.stream()
                .map(ExerciseResult::getFormWarnings)
                .filter(value -> value != null && !value.isBlank())
                .flatMap(value -> Arrays.stream(value.split("\\|")))
                .filter(value -> !value.isBlank())
                .map(this::label)
                .forEach(formFlags::add);

        BigDecimal calories = sessions.stream()
                .map(WorkoutSession::getCaloriesBurned)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(0, RoundingMode.HALF_UP);
        return new CoachContext.WorkoutFacts(
                sessions.size(),
                (int) sessions.stream().filter(session -> session.getStatus() == SessionStatus.COMPLETED).count(),
                (int) sessions.stream().filter(session -> session.getStatus() == SessionStatus.SKIPPED).count(),
                completedResults.size(),
                calories,
                averageFormScore,
                List.copyOf(formFlags));
    }

    private CoachContext.WellnessFacts wellnessFacts(
            UserProfile profile,
            WellnessProgressResponse progress,
            int periodDays
    ) {
        List<WellnessDayResponse> days = progress.dailyBreakdown();
        WellnessDayResponse today = days.isEmpty() ? null : days.getLast();
        return new CoachContext.WellnessFacts(
                progress.averageSleepMinutes(),
                profile.getSleepTargetMinutes(),
                progress.averageEnergyLevel(),
                progress.hydrationGoalDays(),
                periodDays,
                today == null ? 0 : today.waterMl(),
                today == null ? valueOrDefault(profile.getDailyWaterTargetMl(), 2000) : today.waterTargetMl(),
                progress.totalNutritionCalories(),
                days.stream().map(WellnessDayResponse::proteinGrams)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                days.stream().map(WellnessDayResponse::carbohydrateGrams)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                days.stream().map(WellnessDayResponse::fatGrams)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private List<String> missingData(
            TodayWorkoutResponse todayWorkout,
            List<WorkoutSession> sessions,
            List<ExerciseResult> results,
            CoachContext.WellnessFacts wellness
    ) {
        List<String> missing = new ArrayList<>();
        if (todayWorkout == null) missing.add("No active workout plan is available");
        if (sessions.isEmpty()) missing.add("No workout session is recorded for this period");
        if (results.stream().noneMatch(result -> result.getFormScore() != null)) {
            missing.add("No camera form score is recorded for this period");
        }
        if (wellness.averageSleepMinutes() == 0) missing.add("No sleep check-in is recorded");
        if (wellness.waterTodayMl() == 0) missing.add("No water intake is recorded today");
        if (wellness.nutritionCalories() == 0) missing.add("No nutrition entry is recorded for this period");
        return missing;
    }

    private List<String> evidence(
            UserProfile profile,
            TodayWorkoutResponse todayWorkout,
            CoachContext.WorkoutFacts workouts,
            CoachContext.WellnessFacts wellness
    ) {
        List<String> evidence = new ArrayList<>();
        evidence.add("Goal: " + label(profile.getFitnessGoal().name()));
        if (todayWorkout != null) {
            evidence.add(todayWorkout.restDay() || todayWorkout.workout() == null
                    ? "Today is a planned recovery day"
                    : "Today: " + todayWorkout.workout().title() + " for about "
                            + todayWorkout.workout().estimatedMinutes() + " minutes");
        }
        if (workouts.recordedSessions() > 0) {
            evidence.add("Workouts: " + workouts.completedWorkouts() + " completed, "
                    + workouts.skippedWorkouts() + " skipped");
            evidence.add("Workout calories: " + workouts.caloriesBurned().toPlainString() + " kcal");
        }
        if (wellness.averageSleepMinutes() > 0) {
            evidence.add("Average sleep: " + durationLabel(wellness.averageSleepMinutes()));
        }
        if (wellness.averageEnergyLevel() > 0) {
            evidence.add("Average energy: " + wellness.averageEnergyLevel() + "/5");
        }
        evidence.add("Hydration today: " + wellness.waterTodayMl() + "/"
                + wellness.waterTargetMl() + " ml");
        if (wellness.nutritionCalories() > 0) {
            evidence.add("Nutrition logged: " + wellness.nutritionCalories() + " kcal");
        }
        if (workouts.averageFormScore() != null) {
            evidence.add("Average camera form score: " + workouts.averageFormScore() + "%");
        }
        return evidence;
    }

    private String durationLabel(long minutes) {
        return (minutes / 60) + "h " + (minutes % 60) + "m";
    }

    private int valueOrDefault(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private String label(String value) {
        return value.toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
