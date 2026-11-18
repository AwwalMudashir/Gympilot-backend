package com.project.gympilot_backend.vision.service;

import com.project.gympilot_backend.common.exception.BadRequestException;
import com.project.gympilot_backend.common.exception.ResourceNotFoundException;
import com.project.gympilot_backend.user.model.User;
import com.project.gympilot_backend.vision.dto.VisionWorkoutSummaryRequest;
import com.project.gympilot_backend.vision.model.ExerciseResult;
import com.project.gympilot_backend.vision.repository.ExerciseResultRepository;
import com.project.gympilot_backend.workout.dto.ExerciseResultResponse;
import com.project.gympilot_backend.workout.model.SessionStatus;
import com.project.gympilot_backend.workout.model.WorkoutPlanItem;
import com.project.gympilot_backend.workout.model.WorkoutSession;
import com.project.gympilot_backend.workout.repository.WorkoutPlanItemRepository;
import com.project.gympilot_backend.workout.repository.WorkoutSessionRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VisionWorkoutResultService {
    private static final String SQUAT_CODE = "BODYWEIGHT_SQUAT";
    private static final String CURL_CODE = "BICEP_CURL";
    private static final Map<String, Set<String>> SUPPORTED_FORM_FLAGS = Map.of(
            SQUAT_CODE, Set.of("INSUFFICIENT_DEPTH", "EXCESSIVE_FORWARD_LEAN"),
            CURL_CODE, Set.of("INCOMPLETE_RANGE", "ELBOW_DRIFT")
    );

    private final WorkoutSessionRepository sessionRepository;
    private final WorkoutPlanItemRepository planItemRepository;
    private final ExerciseResultRepository resultRepository;
    private final ZoneId applicationZone;

    public VisionWorkoutResultService(
            WorkoutSessionRepository sessionRepository,
            WorkoutPlanItemRepository planItemRepository,
            ExerciseResultRepository resultRepository,
            @Value("${app.time-zone:Africa/Lagos}") String timeZone
    ) {
        this.sessionRepository = sessionRepository;
        this.planItemRepository = planItemRepository;
        this.resultRepository = resultRepository;
        this.applicationZone = ZoneId.of(timeZone);
    }

    @Transactional
    public ExerciseResultResponse save(User user, VisionWorkoutSummaryRequest request) {
        WorkoutSession session = requireOwnedSession(user, request.workoutSessionId());
        requireActiveToday(session);

        WorkoutPlanItem item = requirePlannedExercise(session, request.exerciseId());
        String exerciseCode = item.getExercise().getCode();
        if (!item.isCameraSupported() || !SUPPORTED_FORM_FLAGS.containsKey(exerciseCode)) {
            throw new BadRequestException(
                    "VISION_EXERCISE_UNSUPPORTED",
                    "Live vision tracking supports Bodyweight Squat and Dumbbell Bicep Curl only");
        }
        if (request.completedSets() > item.getSets()
                || request.completedRepetitions() > item.getTargetRepetitions()) {
            throw new BadRequestException(
                    "VISION_RESULT_EXCEEDS_PLAN",
                    "The vision result exceeds the sets or repetitions planned for this exercise");
        }

        List<String> formFlags = normalizeFormFlags(exerciseCode, request.formFlags());
        ExerciseResult result = resultRepository
                .findByWorkoutSessionIdAndExerciseId(session.getId(), item.getExercise().getId())
                .orElseGet(() -> ExerciseResult.builder()
                        .workoutSession(session)
                        .exercise(item.getExercise())
                        .build());

        result.setPlannedSets(item.getSets());
        result.setPlannedRepetitions(item.getTargetRepetitions());
        result.setCompletedSets(Math.max(valueOrZero(result.getCompletedSets()), request.completedSets()));
        result.setCompletedRepetitions(Math.max(
                valueOrZero(result.getCompletedRepetitions()), request.completedRepetitions()));
        result.setAverageRepDurationSeconds(request.averageTempoSeconds());
        result.setFormScore(request.score());
        result.setFormWarnings(String.join("|", formFlags));
        result.setSkipped(false);

        return ExerciseResultResponse.from(resultRepository.save(result));
    }

    private WorkoutSession requireOwnedSession(User user, UUID sessionId) {
        WorkoutSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout session was not found"));
        if (!session.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Workout session was not found");
        }
        return session;
    }

    private void requireActiveToday(WorkoutSession session) {
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new BadRequestException(
                    "VISION_WORKOUT_NOT_ACTIVE", "Start today's workout before using vision tracking");
        }
        if (!LocalDate.now(applicationZone).equals(session.getScheduledDate())) {
            throw new BadRequestException(
                    "WORKOUT_NOT_SCHEDULED_TODAY",
                    "Vision tracking is available only on the workout's scheduled date");
        }
    }

    private WorkoutPlanItem requirePlannedExercise(WorkoutSession session, UUID exerciseId) {
        if (session.getWorkoutPlan() == null) {
            throw new ResourceNotFoundException("Workout plan was not found for this session");
        }
        return planItemRepository
                .findAllByWorkoutPlanIdOrderByDayOfWeekAscItemOrderAsc(session.getWorkoutPlan().getId())
                .stream()
                .filter(item -> item.getDayOfWeek() == session.getWorkoutDay())
                .filter(item -> item.getExercise().getId().equals(exerciseId))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "EXERCISE_NOT_IN_SESSION", "This exercise is not part of the active workout"));
    }

    private List<String> normalizeFormFlags(String exerciseCode, List<String> submittedFlags) {
        if (submittedFlags == null || submittedFlags.isEmpty()) return List.of();

        Set<String> supportedFlags = SUPPORTED_FORM_FLAGS.getOrDefault(exerciseCode, Set.of());
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String submittedFlag : submittedFlags) {
            if (submittedFlag == null || submittedFlag.isBlank()) continue;
            String flag = submittedFlag.trim().toUpperCase(Locale.ROOT);
            if (!supportedFlags.contains(flag)) {
                throw new BadRequestException(
                        "VISION_FORM_FLAG_INVALID", "The vision result contains an unsupported form flag");
            }
            normalized.add(flag);
        }
        return List.copyOf(normalized);
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }
}
