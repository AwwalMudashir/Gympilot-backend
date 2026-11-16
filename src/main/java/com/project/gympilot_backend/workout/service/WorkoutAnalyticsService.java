package com.project.gympilot_backend.workout.service;

import com.project.gympilot_backend.user.model.User;
import com.project.gympilot_backend.vision.model.ExerciseResult;
import com.project.gympilot_backend.vision.repository.ExerciseResultRepository;
import com.project.gympilot_backend.workout.dto.DailyCalorieResponse;
import com.project.gympilot_backend.workout.dto.ExerciseCalorieResponse;
import com.project.gympilot_backend.workout.dto.WorkoutAnalyticsResponse;
import com.project.gympilot_backend.workout.model.SessionStatus;
import com.project.gympilot_backend.workout.model.WorkoutSession;
import com.project.gympilot_backend.workout.repository.WorkoutSessionRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkoutAnalyticsService {
    private final WorkoutSessionRepository sessionRepository;
    private final ExerciseResultRepository resultRepository;
    private final ZoneId applicationZone;

    public WorkoutAnalyticsService(
            WorkoutSessionRepository sessionRepository,
            ExerciseResultRepository resultRepository,
            @Value("${app.time-zone:Africa/Lagos}") String timeZone
    ) {
        this.sessionRepository = sessionRepository;
        this.resultRepository = resultRepository;
        this.applicationZone = ZoneId.of(timeZone);
    }

    @Transactional(readOnly = true)
    public WorkoutAnalyticsResponse currentWeek(User user) {
        LocalDate today = LocalDate.now(applicationZone);
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        List<WorkoutSession> sessions = sessionRepository
                .findAllByUserIdAndScheduledDateBetweenOrderByScheduledDateAscCreatedAtAsc(
                        user.getId(), weekStart, weekEnd)
                .stream()
                .filter(this::countsTowardsCalories)
                .toList();

        Map<LocalDate, List<WorkoutSession>> byDate = new LinkedHashMap<>();
        for (int offset = 0; offset < 7; offset++) {
            byDate.put(weekStart.plusDays(offset), new ArrayList<>());
        }
        sessions.forEach(session -> byDate
                .computeIfAbsent(session.getScheduledDate(), ignored -> new ArrayList<>())
                .add(session));

        List<DailyCalorieResponse> daily = byDate.entrySet().stream()
                .map(entry -> new DailyCalorieResponse(
                        entry.getKey(),
                        entry.getValue().stream()
                                .map(this::sessionCalories)
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                        (int) entry.getValue().stream()
                                .filter(session -> session.getStatus() == SessionStatus.COMPLETED)
                                .count()))
                .toList();

        BigDecimal weekCalories = daily.stream()
                .map(DailyCalorieResponse::caloriesBurned)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal todayCalories = daily.stream()
                .filter(day -> day.date().equals(today))
                .map(DailyCalorieResponse::caloriesBurned)
                .findFirst()
                .orElse(BigDecimal.ZERO);

        List<UUID> sessionIds = sessions.stream().map(WorkoutSession::getId).toList();
        List<ExerciseResult> results = sessionIds.isEmpty()
                ? List.of() : resultRepository.findAllByWorkoutSessionIdIn(sessionIds).stream()
                .filter(result -> !result.isSkipped() && valueOrZero(result.getCompletedSets()) > 0)
                .toList();

        Map<UUID, ExerciseAccumulator> exerciseTotals = new LinkedHashMap<>();
        results.forEach(result -> exerciseTotals
                .computeIfAbsent(result.getExercise().getId(), ignored ->
                        new ExerciseAccumulator(result.getExercise().getName()))
                .add(result));
        List<ExerciseCalorieResponse> exerciseBreakdown = exerciseTotals.entrySet().stream()
                .map(entry -> entry.getValue().response(entry.getKey()))
                .sorted(Comparator.comparing(ExerciseCalorieResponse::caloriesBurned).reversed())
                .toList();

        return new WorkoutAnalyticsResponse(
                weekStart,
                todayCalories,
                weekCalories,
                (int) sessions.stream()
                        .filter(session -> session.getStatus() == SessionStatus.COMPLETED)
                        .count(),
                results.size(),
                daily,
                exerciseBreakdown);
    }

    private boolean countsTowardsCalories(WorkoutSession session) {
        return session.getStatus() == SessionStatus.IN_PROGRESS
                || session.getStatus() == SessionStatus.COMPLETED;
    }

    private BigDecimal sessionCalories(WorkoutSession session) {
        return session.getCaloriesBurned() == null ? BigDecimal.ZERO : session.getCaloriesBurned();
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private static final class ExerciseAccumulator {
        private final String name;
        private BigDecimal calories = BigDecimal.ZERO;
        private int completedSets;

        private ExerciseAccumulator(String name) {
            this.name = name;
        }

        private void add(ExerciseResult result) {
            calories = calories.add(result.getCaloriesBurned() == null
                    ? BigDecimal.ZERO : result.getCaloriesBurned());
            completedSets += result.getCompletedSets() == null ? 0 : result.getCompletedSets();
        }

        private ExerciseCalorieResponse response(UUID exerciseId) {
            return new ExerciseCalorieResponse(exerciseId, name, calories, completedSets);
        }
    }
}
