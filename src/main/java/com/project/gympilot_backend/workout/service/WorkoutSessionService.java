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
import com.project.gympilot_backend.workout.model.SessionStatus;
import com.project.gympilot_backend.workout.model.WorkoutPlan;
import com.project.gympilot_backend.workout.model.WorkoutPlanItem;
import com.project.gympilot_backend.workout.model.WorkoutSession;
import com.project.gympilot_backend.workout.repository.WorkoutPlanItemRepository;
import com.project.gympilot_backend.workout.repository.WorkoutPlanRepository;
import com.project.gympilot_backend.workout.repository.WorkoutSessionRepository;
import java.time.DayOfWeek;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkoutSessionService {
    private final WorkoutSessionRepository sessionRepository;
    private final WorkoutPlanRepository planRepository;
    private final WorkoutPlanItemRepository itemRepository;
    private final ExerciseResultRepository resultRepository;

    public WorkoutSessionService(
            WorkoutSessionRepository sessionRepository,
            WorkoutPlanRepository planRepository,
            WorkoutPlanItemRepository itemRepository,
            ExerciseResultRepository resultRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.planRepository = planRepository;
        this.itemRepository = itemRepository;
        this.resultRepository = resultRepository;
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
        if (session.getStatus() == SessionStatus.COMPLETED || session.getStatus() == SessionStatus.SKIPPED
                || session.getStatus() == SessionStatus.CANCELLED) {
            throw invalidState("Only a scheduled workout can be started");
        }
        if (session.getStartedAt() == null) session.setStartedAt(Instant.now());
        session.setStatus(SessionStatus.IN_PROGRESS);
        return response(sessionRepository.save(session));
    }

    @Transactional
    public WorkoutSessionResponse complete(User user, UUID sessionId, CompleteWorkoutSessionRequest request) {
        WorkoutSession session = requireOwnedSession(user, sessionId);
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw invalidState("Start this workout before marking it complete");
        }
        List<WorkoutPlanItem> planItems = requirePlanItems(
                session.getWorkoutPlan().getId(), DayOfWeek.of(session.getWorkoutDay()));
        Map<UUID, ExerciseCompletionRequest> supplied = request.results() == null ? Map.of()
                : request.results().stream().collect(Collectors.toMap(
                        ExerciseCompletionRequest::exerciseId, Function.identity(), (first, ignored) -> first));
        var plannedExerciseIds = planItems.stream()
                .map(item -> item.getExercise().getId())
                .collect(Collectors.toSet());
        if (!plannedExerciseIds.containsAll(supplied.keySet())) {
            throw new BadRequestException(
                    "EXERCISE_NOT_IN_SESSION", "A submitted exercise is not part of this workout");
        }

        List<ExerciseResult> results = planItems.stream().map(item -> {
            ExerciseCompletionRequest completion = supplied.get(item.getExercise().getId());
            return ExerciseResult.builder()
                    .workoutSession(session)
                    .exercise(item.getExercise())
                    .plannedSets(item.getSets())
                    .completedSets(completion == null ? item.getSets() : completion.completedSets())
                    .plannedRepetitions(item.getTargetRepetitions())
                    .completedRepetitions(completion == null
                            ? item.getTargetRepetitions() : completion.completedRepetitions())
                    .build();
        }).toList();
        resultRepository.saveAll(results);
        session.setStatus(SessionStatus.COMPLETED);
        session.setCompletedAt(Instant.now());
        session.setDifficultyRating(request.difficultyRating());
        session.setNotes(trimToNull(request.notes()));
        return response(sessionRepository.save(session));
    }

    @Transactional
    public WorkoutSessionResponse skip(User user, UUID sessionId, SkipWorkoutSessionRequest request) {
        WorkoutSession session = requireOwnedSession(user, sessionId);
        if (session.getStatus() == SessionStatus.COMPLETED || session.getStatus() == SessionStatus.SKIPPED) {
            throw invalidState("This workout has already been finalised");
        }
        session.setStatus(SessionStatus.SKIPPED);
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
                session.getScheduledDate(), session.getStartedAt(), session.getCompletedAt(), session.getStatus(),
                session.getDifficultyRating(), session.getNotes(), results);
    }

    private BadRequestException invalidState(String message) {
        return new BadRequestException("WORKOUT_SESSION_STATE_INVALID", message);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
