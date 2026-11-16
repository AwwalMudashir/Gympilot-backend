package com.project.gympilot_backend.workout.service;

import com.project.gympilot_backend.common.exception.ResourceNotFoundException;
import com.project.gympilot_backend.profile.model.FitnessGoal;
import com.project.gympilot_backend.profile.model.UserProfile;
import com.project.gympilot_backend.profile.repository.UserProfileRepository;
import com.project.gympilot_backend.user.model.User;
import com.project.gympilot_backend.workout.dto.TodayWorkoutResponse;
import com.project.gympilot_backend.workout.dto.WorkoutDayResponse;
import com.project.gympilot_backend.workout.dto.WorkoutPlanItemResponse;
import com.project.gympilot_backend.workout.dto.WorkoutPlanResponse;
import com.project.gympilot_backend.workout.model.Exercise;
import com.project.gympilot_backend.workout.model.PlanStatus;
import com.project.gympilot_backend.workout.model.WorkoutPlan;
import com.project.gympilot_backend.workout.model.WorkoutPlanItem;
import com.project.gympilot_backend.workout.repository.ExerciseRepository;
import com.project.gympilot_backend.workout.repository.WorkoutPlanItemRepository;
import com.project.gympilot_backend.workout.repository.WorkoutPlanRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkoutPlanService {
    private static final int MAX_EXERCISES_PER_SESSION = 8;
    private static final List<DayOfWeek> DEFAULT_DAYS = List.of(
            DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY,
            DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    private final WorkoutPlanRepository planRepository;
    private final WorkoutPlanItemRepository itemRepository;
    private final ExerciseRepository exerciseRepository;
    private final UserProfileRepository profileRepository;
    private final PlanExplanationService explanationService;

    public WorkoutPlanService(
            WorkoutPlanRepository planRepository,
            WorkoutPlanItemRepository itemRepository,
            ExerciseRepository exerciseRepository,
            UserProfileRepository profileRepository,
            PlanExplanationService explanationService
    ) {
        this.planRepository = planRepository;
        this.itemRepository = itemRepository;
        this.exerciseRepository = exerciseRepository;
        this.profileRepository = profileRepository;
        this.explanationService = explanationService;
    }

    @Transactional(readOnly = true)
    public WorkoutPlanResponse current(User user) {
        LocalDate weekStart = weekStart(LocalDate.now(ZoneOffset.UTC));
        WorkoutPlan plan = planRepository
                .findFirstByUserIdAndWeekStartDateAndStatusOrderByCreatedAtDesc(
                        user.getId(), weekStart, PlanStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("No workout plan exists for this week"));
        return response(plan);
    }

    @Transactional
    public WorkoutPlanResponse generate(User user) {
        LocalDate weekStart = weekStart(LocalDate.now(ZoneOffset.UTC));
        var existing = planRepository.findFirstByUserIdAndWeekStartDateAndStatusOrderByCreatedAtDesc(
                user.getId(), weekStart, PlanStatus.ACTIVE);
        if (existing.isPresent()) return response(existing.get());
        return buildPlan(user, weekStart);
    }

    @Transactional
    public WorkoutPlanResponse regenerate(User user) {
        LocalDate weekStart = weekStart(LocalDate.now(ZoneOffset.UTC));
        planRepository.findFirstByUserIdAndWeekStartDateAndStatusOrderByCreatedAtDesc(
                user.getId(), weekStart, PlanStatus.ACTIVE).ifPresent(plan -> {
                    plan.setStatus(PlanStatus.ARCHIVED);
                    planRepository.saveAndFlush(plan);
                });
        return buildPlan(user, weekStart);
    }

    private WorkoutPlanResponse buildPlan(User user, LocalDate weekStart) {
        UserProfile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Complete your fitness profile before generating a plan"));
        List<Exercise> eligible = eligibleExercises(profile);
        if (eligible.size() < 3) {
            throw new ResourceNotFoundException("Not enough exercises are available for the selected equipment");
        }

        WorkoutPlan plan = planRepository.save(WorkoutPlan.builder()
                .user(user)
                .weekStartDate(weekStart)
                .status(PlanStatus.ACTIVE)
                .explanation("Generating your personalised plan explanation.")
                .build());

        List<DayOfWeek> days = workoutDays(profile);
        SessionPrescription prescription = sessionPrescription(profile, eligible.size());
        List<WorkoutPlanItem> items = new ArrayList<>();
        for (int dayIndex = 0; dayIndex < days.size(); dayIndex++) {
            for (int order = 0; order < prescription.exerciseCount(); order++) {
                Exercise exercise = eligible.get((dayIndex * 2 + order) % eligible.size());
                items.add(WorkoutPlanItem.builder()
                        .workoutPlan(plan)
                        .exercise(exercise)
                        .dayOfWeek(days.get(dayIndex).getValue())
                        .sets(prescription.sets())
                        .targetRepetitions(prescription.repetitions())
                        .restSeconds(prescription.restSeconds())
                        .itemOrder(order + 1)
                        .cameraSupported(exercise.isCameraSupported())
                        .build());
            }
        }
        itemRepository.saveAll(items);
        plan.setExplanation(explanationService.explain(profile, items));
        planRepository.save(plan);
        return response(plan);
    }

    @Transactional(readOnly = true)
    public TodayWorkoutResponse today(User user) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        WorkoutPlanResponse plan = current(user);
        WorkoutDayResponse workout = plan.days().stream()
                .filter(day -> day.day() == today.getDayOfWeek())
                .findFirst()
                .orElse(null);
        return new TodayWorkoutResponse(plan.id(), today, workout == null, workout);
    }

    private WorkoutPlanResponse response(WorkoutPlan plan) {
        BigDecimal weightKg = profileRepository.findByUserId(plan.getUser().getId())
                .map(UserProfile::getWeightKg)
                .orElse(BigDecimal.valueOf(70));
        List<WorkoutPlanItem> items = itemRepository
                .findAllByWorkoutPlanIdOrderByDayOfWeekAscItemOrderAsc(plan.getId());
        Map<DayOfWeek, List<WorkoutPlanItem>> grouped = new LinkedHashMap<>();
        items.stream().sorted(Comparator.comparingInt(WorkoutPlanItem::getDayOfWeek)
                        .thenComparingInt(WorkoutPlanItem::getItemOrder))
                .forEach(item -> grouped.computeIfAbsent(DayOfWeek.of(item.getDayOfWeek()), ignored -> new ArrayList<>())
                        .add(item));
        List<WorkoutDayResponse> days = grouped.entrySet().stream()
                .map(entry -> {
                    List<WorkoutPlanItemResponse> exerciseResponses = entry.getValue().stream()
                            .map(item -> WorkoutPlanItemResponse.from(item, weightKg))
                            .toList();
                    BigDecimal estimatedCalories = exerciseResponses.stream()
                            .map(WorkoutPlanItemResponse::estimatedCalories)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new WorkoutDayResponse(
                            entry.getKey(), dayTitle(entry.getValue()),
                            WorkoutTimingCalculator.estimateMinutes(entry.getValue()),
                            estimatedCalories, exerciseResponses);
                })
                .toList();
        return new WorkoutPlanResponse(
                plan.getId(), plan.getWeekStartDate(), plan.getStatus(), plan.getExplanation(),
                days.size(), items.size(), days);
    }

    private List<Exercise> eligibleExercises(UserProfile profile) {
        Set<String> equipment = profile.getAvailableEquipment().stream()
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());
        return exerciseRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .filter(exercise -> exercise.getEquipment() == null
                        || exercise.getEquipment().equalsIgnoreCase("Bodyweight")
                        || equipment.contains(exercise.getEquipment().toLowerCase(Locale.ROOT)))
                .toList();
    }

    private List<DayOfWeek> workoutDays(UserProfile profile) {
        List<DayOfWeek> preferred = profile.getPreferredWorkoutDays().stream().sorted().toList();
        if (!preferred.isEmpty()) return preferred;

        int requestedCount = profile.getDaysPerWeek() == null ? 3 : profile.getDaysPerWeek();
        int count = Math.max(1, Math.min(7, requestedCount));
        return DEFAULT_DAYS.stream().limit(count).sorted().toList();
    }

    private SessionPrescription sessionPrescription(UserProfile profile, int eligibleExerciseCount) {
        int targetMinutes = profile.getWorkoutDurationMinutes() == null
                ? 30
                : Math.max(10, profile.getWorkoutDurationMinutes());
        int targetSeconds = targetMinutes * 60;
        int repetitions = repetitions(profile);
        int restSeconds = GoalRestDurationPolicy.secondsFor(profile.getFitnessGoal());
        int maxSets = switch (profile.getExperienceLevel()) {
            case BEGINNER -> 3;
            case INTERMEDIATE -> 4;
            case ADVANCED -> 5;
        };
        int maxExercises = Math.max(1, Math.min(MAX_EXERCISES_PER_SESSION, eligibleExerciseCount));
        SessionPrescription best = null;

        for (int sets = 2; sets <= maxSets; sets++) {
            for (int exerciseCount = 1; exerciseCount <= maxExercises; exerciseCount++) {
                int estimatedSeconds = WorkoutTimingCalculator.estimateSeconds(
                        exerciseCount, sets, repetitions, restSeconds);
                if (estimatedSeconds > targetSeconds) continue;

                SessionPrescription candidate = new SessionPrescription(
                        exerciseCount, sets, repetitions, restSeconds, estimatedSeconds);
                if (isBetterFit(candidate, best)) best = candidate;
            }
        }

        if (best != null) return best;
        int estimatedSeconds = WorkoutTimingCalculator.estimateSeconds(
                1, 2, repetitions, restSeconds);
        return new SessionPrescription(1, 2, repetitions, restSeconds, estimatedSeconds);
    }

    private int repetitions(UserProfile profile) {
        int reps = 10;
        if (profile.getFitnessGoal() == FitnessGoal.STRENGTH) {
            reps = 6;
        } else if (profile.getFitnessGoal() == FitnessGoal.WEIGHT_LOSS) {
            reps = 12;
        }
        return reps;
    }

    private boolean isBetterFit(SessionPrescription candidate, SessionPrescription current) {
        if (current == null) return true;
        if (candidate.estimatedSeconds() != current.estimatedSeconds()) {
            return candidate.estimatedSeconds() > current.estimatedSeconds();
        }
        if (candidate.sets() != current.sets()) return candidate.sets() > current.sets();
        if (candidate.exerciseCount() != current.exerciseCount()) {
            return candidate.exerciseCount() < current.exerciseCount();
        }
        return candidate.restSeconds() > current.restSeconds();
    }

    private String dayTitle(List<WorkoutPlanItem> items) {
        if (items.isEmpty()) return "Recovery day";
        String first = items.get(0).getExercise().getMuscleGroup();
        return first == null ? "Full body training" : first + " focus";
    }

    private LocalDate weekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private record SessionPrescription(
            int exerciseCount,
            int sets,
            int repetitions,
            int restSeconds,
            int estimatedSeconds
    ) {}
}
