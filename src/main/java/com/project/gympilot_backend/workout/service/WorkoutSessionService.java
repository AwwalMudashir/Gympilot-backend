package com.project.gympilot_backend.workout.service;

import com.project.gympilot_backend.common.exception.BadRequestException;
import com.project.gympilot_backend.common.exception.ResourceNotFoundException;
import com.project.gympilot_backend.user.model.User;
import com.project.gympilot_backend.vision.model.ExerciseResult;
import com.project.gympilot_backend.vision.repository.ExerciseResultRepository;
import com.project.gympilot_backend.workout.dto.CompleteWorkoutSessionRequest;
import com.project.gympilot_backend.workout.dto.CreateWorkoutSessionRequest;
import com.project.gympilot_backend.workout.dto.ExerciseCompletionRequest;
import com.project.gympilot_backend.workout.dto.ExerciseResultResponse;
import com.project.gympilot_backend.workout.dto.RescheduleWorkoutSessionRequest;
import com.project.gympilot_backend.workout.dto.SkipWorkoutSessionRequest;
import com.project.gympilot_backend.workout.dto.WorkoutSessionResponse;
import com.project.gympilot_backend.workout.dto.WorkoutProgressRequest;
import com.project.gympilot_backend.workout.model.SessionStatus;
import com.project.gympilot_backend.workout.model.WorkoutPlan;
import com.project.gympilot_backend.workout.model.WorkoutPlanItem;
import com.project.gympilot_backend.workout.model.WorkoutSession;
import com.project.gympilot_backend.workout.model.WorkoutPhase;
import com.project.gympilot_backend.workout.repository.WorkoutPlanItemRepository;
import com.project.gympilot_backend.workout.repository.WorkoutPlanRepository;
import com.project.gympilot_backend.workout.repository.WorkoutSessionRepository;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

@Service
public class WorkoutSessionService {
    private final WorkoutSessionRepository sessionRepository;
    private final WorkoutPlanRepository planRepository;
    private final WorkoutPlanItemRepository itemRepository;
    private final ExerciseResultRepository resultRepository;
    private final ZoneId applicationZone;

    public WorkoutSessionService(
            WorkoutSessionRepository sessionRepository,
            WorkoutPlanRepository planRepository,
            WorkoutPlanItemRepository itemRepository,
            ExerciseResultRepository resultRepository,
            @Value("${app.time-zone:Africa/Lagos}") String timeZone
    ) {
        this.sessionRepository = sessionRepository;
        this.planRepository = planRepository;
        this.itemRepository = itemRepository;
        this.resultRepository = resultRepository;
        this.applicationZone = ZoneId.of(timeZone);
    }

    @Transactional
    public WorkoutSessionResponse createOrGet(User user, CreateWorkoutSessionRequest request) {
        WorkoutPlan plan = requireOwnedPlan(user, request.workoutPlanId());
        requirePlanItems(plan.getId(), request.workoutDay());
        WorkoutSession session = sessionRepository.findByUserIdAndWorkoutPlanIdAndWorkoutDay(
                        user.getId(), plan.getId(), request.workoutDay().getValue())
                .orElseGet(() -> sessionRepository.save(WorkoutSession.builder()
                        .user(user)
                        .workoutPlan(plan)
                        .workoutDay(request.workoutDay().getValue())
                        .scheduledDate(request.scheduledDate())
                        .status(SessionStatus.SCHEDULED)
                        .build()));
        return response(session);
    }

    @Transactional
    public WorkoutSessionResponse start(User user, UUID sessionId) {
        WorkoutSession session = requireOwnedSession(user, sessionId);
        requireScheduledToday(session);
        if (session.getStatus() == SessionStatus.COMPLETED || session.getStatus() == SessionStatus.SKIPPED
                || session.getStatus() == SessionStatus.CANCELLED) {
            throw invalidState("Only a scheduled or paused workout can be started");
        }
        Instant now = Instant.now();
        if (session.getStartedAt() == null) session.setStartedAt(now);
        if (session.getActiveSince() == null) session.setActiveSince(now);
        session.setStatus(SessionStatus.IN_PROGRESS);
        return response(sessionRepository.save(session));
    }

    @Transactional
    public WorkoutSessionResponse pause(User user, UUID sessionId, WorkoutProgressRequest request) {
        WorkoutSession session = requireOwnedSession(user, sessionId);
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw invalidState("Only an in-progress workout can be paused");
        }

        List<WorkoutPlanItem> planItems = requirePlanItems(
                session.getWorkoutPlan().getId(), DayOfWeek.of(session.getWorkoutDay()));
        if (request.currentExerciseIndex() >= planItems.size()) {
            throw new BadRequestException(
                    "WORKOUT_PROGRESS_INVALID", "The saved exercise position is outside this workout");
        }
        WorkoutPlanItem currentItem = planItems.get(request.currentExerciseIndex());
        if (request.currentSetNumber() > currentItem.getSets()) {
            throw new BadRequestException(
                    "WORKOUT_PROGRESS_INVALID", "The saved set position is outside this exercise");
        }

        saveProgress(session, planItems, request.results(), false);
        session.setElapsedSeconds(currentElapsedSeconds(session));
        session.setActiveSince(null);
        session.setCurrentExerciseIndex(request.currentExerciseIndex());
        session.setCurrentSetNumber(request.currentSetNumber());
        session.setCurrentPhase(request.currentPhase());
        session.setPhaseRemainingSeconds(request.phaseRemainingSeconds());
        return response(sessionRepository.save(session));
    }

    @Transactional
    public WorkoutSessionResponse complete(User user, UUID sessionId, CompleteWorkoutSessionRequest request) {
        WorkoutSession session = requireOwnedSession(user, sessionId);
        requireScheduledToday(session);
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw invalidState("Start this workout before marking it complete");
        }
        List<WorkoutPlanItem> planItems = requirePlanItems(
                session.getWorkoutPlan().getId(), DayOfWeek.of(session.getWorkoutDay()));
        int elapsedSeconds = currentElapsedSeconds(session);
        enforceMinimumDuration(planItems, request.results(), elapsedSeconds);
        saveProgress(session, planItems, request.results(), true);
        session.setElapsedSeconds(elapsedSeconds);
        session.setActiveSince(null);
        session.setStatus(SessionStatus.COMPLETED);
        session.setCurrentPhase(WorkoutPhase.COOLDOWN);
        session.setPhaseRemainingSeconds(0);
        session.setCompletedAt(Instant.now());
        session.setDifficultyRating(request.difficultyRating());
        session.setNotes(trimToNull(request.notes()));
        return response(sessionRepository.save(session));
    }

    @Transactional
    public WorkoutSessionResponse skip(User user, UUID sessionId, SkipWorkoutSessionRequest request) {
        WorkoutSession session = requireOwnedSession(user, sessionId);
        requireScheduledToday(session);
        if (session.getStatus() == SessionStatus.COMPLETED || session.getStatus() == SessionStatus.SKIPPED) {
            throw invalidState("This workout has already been finalised");
        }
        session.setStatus(SessionStatus.SKIPPED);
        session.setElapsedSeconds(currentElapsedSeconds(session));
        session.setActiveSince(null);
        session.setCompletedAt(Instant.now());
        session.setNotes(trimToNull(request.notes()));
        return response(sessionRepository.save(session));
    }

    @Transactional
    public WorkoutSessionResponse reschedule(
            User user, UUID sessionId, RescheduleWorkoutSessionRequest request
    ) {
        WorkoutSession session = requireOwnedSession(user, sessionId);
        if (session.getStatus() == SessionStatus.COMPLETED || session.getStatus() == SessionStatus.SKIPPED) {
            throw invalidState("A completed or skipped workout cannot be rescheduled");
        }
        session.setScheduledDate(request.scheduledDate());
        session.setStatus(SessionStatus.SCHEDULED);
        session.setStartedAt(null);
        session.setActiveSince(null);
        session.setElapsedSeconds(0);
        session.setCurrentExerciseIndex(0);
        session.setCurrentSetNumber(1);
        session.setCurrentPhase(WorkoutPhase.WARM_UP);
        session.setPhaseRemainingSeconds(WorkoutTimingCalculator.WARM_UP_SECONDS);
        resultRepository.deleteAllByWorkoutSessionId(session.getId());
        return response(sessionRepository.save(session));
    }

    @Transactional(readOnly = true)
    public List<WorkoutSessionResponse> history(User user) {
        return sessionRepository.findAllByUserIdOrderByScheduledDateDescCreatedAtDesc(user.getId()).stream()
                .filter(session -> session.getStatus() == SessionStatus.COMPLETED
                        || session.getStatus() == SessionStatus.SKIPPED)
                .map(this::response)
                .toList();
    }

    private WorkoutPlan requireOwnedPlan(User user, UUID planId) {
        WorkoutPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout plan was not found"));
        if (!plan.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Workout plan was not found");
        }
        return plan;
    }

    private WorkoutSession requireOwnedSession(User user, UUID sessionId) {
        WorkoutSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout session was not found"));
        if (!session.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Workout session was not found");
        }
        return session;
    }

    private List<WorkoutPlanItem> requirePlanItems(UUID planId, DayOfWeek day) {
        List<WorkoutPlanItem> items = itemRepository
                .findAllByWorkoutPlanIdOrderByDayOfWeekAscItemOrderAsc(planId).stream()
                .filter(item -> item.getDayOfWeek() == day.getValue())
                .toList();
        if (items.isEmpty()) throw new ResourceNotFoundException("No workout is planned for that day");
        return items;
    }

    private WorkoutSessionResponse response(WorkoutSession session) {
        List<WorkoutPlanItem> items = itemRepository
                .findAllByWorkoutPlanIdOrderByDayOfWeekAscItemOrderAsc(session.getWorkoutPlan().getId()).stream()
                .filter(item -> item.getDayOfWeek() == session.getWorkoutDay())
                .toList();
        String title = items.isEmpty() || items.getFirst().getExercise().getMuscleGroup() == null
                ? "Workout" : items.getFirst().getExercise().getMuscleGroup() + " focus";
        List<ExerciseResultResponse> results = resultRepository.findAllByWorkoutSessionId(session.getId()).stream()
                .map(ExerciseResultResponse::from)
                .toList();
        return new WorkoutSessionResponse(
                session.getId(), session.getWorkoutPlan().getId(), DayOfWeek.of(session.getWorkoutDay()), title,
                session.getScheduledDate(), session.getStartedAt(), session.getCompletedAt(), session.getActiveSince(),
                session.getStatus(), session.getDifficultyRating(), session.getNotes(),
                session.getCurrentExerciseIndex(), session.getCurrentSetNumber(), session.getCurrentPhase(),
                session.getPhaseRemainingSeconds(), session.getElapsedSeconds(), results);
    }

    private BadRequestException invalidState(String message) {
        return new BadRequestException("WORKOUT_SESSION_STATE_INVALID", message);
    }

    private void enforceMinimumDuration(
            List<WorkoutPlanItem> planItems,
            List<ExerciseCompletionRequest> requestedResults,
            int elapsedSeconds
    ) {
        Set<UUID> skippedExerciseIds = requestedResults == null ? Set.of()
                : requestedResults.stream()
                .filter(ExerciseCompletionRequest::skipped)
                .map(ExerciseCompletionRequest::exerciseId)
                .collect(Collectors.toSet());
        List<WorkoutPlanItem> performedItems = planItems.stream()
                .filter(item -> !skippedExerciseIds.contains(item.getExercise().getId()))
                .toList();
        long requiredSeconds = WorkoutTimingCalculator.estimateSeconds(performedItems);
        if (elapsedSeconds >= requiredSeconds) return;

        long remainingSeconds = requiredSeconds - elapsedSeconds;
        long remainingMinutes = Math.max(1, (long) Math.ceil(remainingSeconds / 60.0));
        throw new BadRequestException(
                "WORKOUT_MINIMUM_TIME_NOT_REACHED",
                "Keep going — about " + remainingMinutes
                        + (remainingMinutes == 1 ? " minute remains" : " minutes remain"));
    }

    private void requireScheduledToday(WorkoutSession session) {
        LocalDate today = LocalDate.now(applicationZone);
        if (today.equals(session.getScheduledDate())) return;

        throw new BadRequestException(
                "WORKOUT_NOT_SCHEDULED_TODAY",
                "This workout is scheduled for " + session.getScheduledDate()
                        + " and can only be performed on that date");
    }

    private int currentElapsedSeconds(WorkoutSession session) {
        long elapsed = session.getElapsedSeconds() == null ? 0 : session.getElapsedSeconds();
        if (session.getActiveSince() != null) {
            elapsed += Math.max(0, Duration.between(session.getActiveSince(), Instant.now()).getSeconds());
        }
        return (int) Math.min(Integer.MAX_VALUE, elapsed);
    }

    private void saveProgress(
            WorkoutSession session,
            List<WorkoutPlanItem> planItems,
            List<ExerciseCompletionRequest> requestedResults,
            boolean requireComplete
    ) {
        Map<UUID, WorkoutPlanItem> planned = planItems.stream().collect(Collectors.toMap(
                item -> item.getExercise().getId(), Function.identity()));
        Map<UUID, ExerciseCompletionRequest> supplied = requestedResults == null ? Map.of()
                : requestedResults.stream().collect(Collectors.toMap(
                        ExerciseCompletionRequest::exerciseId, Function.identity(), (first, ignored) -> first));

        if (!planned.keySet().containsAll(supplied.keySet())) {
            throw new BadRequestException(
                    "EXERCISE_NOT_IN_SESSION", "A submitted exercise is not part of this workout");
        }
        if (requireComplete && !supplied.keySet().containsAll(planned.keySet())) {
            throw new BadRequestException(
                    "WORKOUT_RESULTS_INCOMPLETE", "Finish every exercise and set before completing this workout");
        }

        boolean invalidProgress = supplied.values().stream().anyMatch(completion -> {
            WorkoutPlanItem item = planned.get(completion.exerciseId());
            return completion.completedSets() > item.getSets()
                    || completion.completedRepetitions() > item.getTargetRepetitions()
                    || (requireComplete && !completion.skipped()
                    && (completion.completedSets() < item.getSets()
                    || completion.completedRepetitions() < item.getTargetRepetitions()));
        });
        if (invalidProgress) {
            throw new BadRequestException(
                    "WORKOUT_RESULTS_INCOMPLETE", "Finish every exercise and set before completing this workout");
        }

        Map<UUID, ExerciseResult> existing = resultRepository.findAllByWorkoutSessionId(session.getId()).stream()
                .collect(Collectors.toMap(result -> result.getExercise().getId(), Function.identity()));
        List<ExerciseResult> results = supplied.values().stream().map(completion -> {
            WorkoutPlanItem item = planned.get(completion.exerciseId());
            ExerciseResult result = existing.getOrDefault(completion.exerciseId(), ExerciseResult.builder()
                    .workoutSession(session)
                    .exercise(item.getExercise())
                    .build());
            result.setPlannedSets(item.getSets());
            result.setCompletedSets(completion.completedSets());
            result.setPlannedRepetitions(item.getTargetRepetitions());
            result.setCompletedRepetitions(completion.completedRepetitions());
            result.setSkipped(completion.skipped());
            return result;
        }).toList();
        if (!results.isEmpty()) resultRepository.saveAll(results);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
