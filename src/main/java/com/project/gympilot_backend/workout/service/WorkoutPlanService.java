package com.project.gympilot_backend.workout.service;

import com.project.gympilot_backend.common.exception.ResourceNotFoundException;
import com.project.gympilot_backend.profile.model.ExperienceLevel;
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
                    planRepository.save(plan);
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
        int exercisesPerDay = exercisesPerDay(profile.getWorkoutDurationMinutes());
        int[] prescription = prescription(profile);
        List<WorkoutPlanItem> items = new ArrayList<>();
        for (int dayIndex = 0; dayIndex < days.size(); dayIndex++) {
            for (int order = 0; order < exercisesPerDay; order++) {
                Exercise exercise = eligible.get((dayIndex * 2 + order) % eligible.size());
                items.add(WorkoutPlanItem.builder()
                        .workoutPlan(plan)
                        .exercise(exercise)
                        .dayOfWeek(days.get(dayIndex).getValue())
                        .sets(prescription[0])
                        .targetRepetitions(prescription[1])
                        .restSeconds(prescription[2])
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
        List<WorkoutPlanItem> items = itemRepository
                .findAllByWorkoutPlanIdOrderByDayOfWeekAscItemOrderAsc(plan.getId());
        Map<DayOfWeek, List<WorkoutPlanItem>> grouped = new LinkedHashMap<>();
        items.stream().sorted(Comparator.comparingInt(WorkoutPlanItem::getDayOfWeek)
                        .thenComparingInt(WorkoutPlanItem::getItemOrder))
                .forEach(item -> grouped.computeIfAbsent(DayOfWeek.of(item.getDayOfWeek()), ignored -> new ArrayList<>())
                        .add(item));
        List<WorkoutDayResponse> days = grouped.entrySet().stream()
                .map(entry -> new WorkoutDayResponse(
                        entry.getKey(), dayTitle(entry.getValue()), estimateMinutes(entry.getValue()),
                        entry.getValue().stream().map(WorkoutPlanItemResponse::from).toList()))
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
        int count = Math.max(1, Math.min(7, profile.getDaysPerWeek()));
        List<DayOfWeek> preferred = profile.getPreferredWorkoutDays().stream().sorted().toList();
        if (preferred.size() >= count) return preferred.subList(0, count);
        List<DayOfWeek> result = new ArrayList<>(preferred);
        DEFAULT_DAYS.stream().filter(day -> !result.contains(day)).limit(count - result.size()).forEach(result::add);
        return result.stream().sorted().toList();
    }

    private int exercisesPerDay(Integer duration) {
        if (duration == null || duration <= 30) return 3;
        if (duration <= 50) return 4;
        return 5;
    }

    private int[] prescription(UserProfile profile) {
        int sets = profile.getExperienceLevel() == ExperienceLevel.BEGINNER ? 2 : 3;
        int reps = 10;
        int rest = 60;
        if (profile.getFitnessGoal() == FitnessGoal.STRENGTH) {
            sets = profile.getExperienceLevel() == ExperienceLevel.BEGINNER ? 3 : 4;
            reps = 6;
            rest = 90;
        } else if (profile.getFitnessGoal() == FitnessGoal.WEIGHT_LOSS) {
            reps = 12;
            rest = 45;
        } else if (profile.getFitnessGoal() == FitnessGoal.MUSCLE_GAIN) {
            reps = 10;
            rest = 75;
        }
        return new int[]{sets, reps, rest};
    }

    private String dayTitle(List<WorkoutPlanItem> items) {
        if (items.isEmpty()) return "Recovery day";
        String first = items.get(0).getExercise().getMuscleGroup();
        return first == null ? "Full body training" : first + " focus";
    }

    private int estimateMinutes(List<WorkoutPlanItem> items) {
        return Math.max(15, items.stream().mapToInt(item -> item.getSets() * 4).sum());
    }

    private LocalDate weekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
