package com.project.gympilot_backend.workout.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.project.gympilot_backend.user.model.User;
import com.project.gympilot_backend.profile.repository.UserProfileRepository;
import com.project.gympilot_backend.vision.model.ExerciseResult;
import com.project.gympilot_backend.vision.repository.ExerciseResultRepository;
import com.project.gympilot_backend.workout.dto.CompleteWorkoutSessionRequest;
import com.project.gympilot_backend.workout.dto.ExerciseCompletionRequest;
import com.project.gympilot_backend.workout.dto.SkipWorkoutSessionRequest;
import com.project.gympilot_backend.workout.dto.WorkoutProgressRequest;
import com.project.gympilot_backend.common.exception.BadRequestException;
import com.project.gympilot_backend.workout.model.DifficultyRating;
import com.project.gympilot_backend.workout.model.Exercise;
import com.project.gympilot_backend.workout.model.SessionStatus;
import com.project.gympilot_backend.workout.model.WorkoutPlan;
import com.project.gympilot_backend.workout.model.WorkoutPlanItem;
import com.project.gympilot_backend.workout.model.WorkoutSession;
import com.project.gympilot_backend.workout.model.WorkoutPhase;
import com.project.gympilot_backend.workout.repository.WorkoutPlanItemRepository;
import com.project.gympilot_backend.workout.repository.WorkoutPlanRepository;
import com.project.gympilot_backend.workout.repository.WorkoutSessionRepository;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkoutSessionServiceTest {
    @Mock WorkoutSessionRepository sessionRepository;
    @Mock WorkoutPlanRepository planRepository;
    @Mock WorkoutPlanItemRepository itemRepository;
    @Mock ExerciseResultRepository resultRepository;
    @Mock UserProfileRepository profileRepository;
    private WorkoutSessionService service;
    private User user;
    private WorkoutSession session;
    private WorkoutPlanItem planItem;

    @BeforeEach
    void setUp() {
        service = new WorkoutSessionService(
                sessionRepository, planRepository, itemRepository, resultRepository,
                profileRepository, "Africa/Lagos");
        user = User.builder().email("member@example.com").firstName("Gym").lastName("Pilot").build();
        user.setId(UUID.randomUUID());
        WorkoutPlan plan = WorkoutPlan.builder().user(user).weekStartDate(LocalDate.now()).build();
        plan.setId(UUID.randomUUID());
        Exercise exercise = Exercise.builder().name("Squat").muscleGroup("Lower body")
                .metValue(java.math.BigDecimal.valueOf(5)).build();
        exercise.setId(UUID.randomUUID());
        planItem = WorkoutPlanItem.builder()
                .workoutPlan(plan).exercise(exercise).dayOfWeek(DayOfWeek.MONDAY.getValue())
                .sets(3).targetRepetitions(10).restSeconds(60).itemOrder(1).build();
        session = WorkoutSession.builder()
                .user(user).workoutPlan(plan).workoutDay(DayOfWeek.MONDAY.getValue())
                .scheduledDate(LocalDate.now(ZoneId.of("Africa/Lagos"))).status(SessionStatus.IN_PROGRESS)
                .startedAt(Instant.now().minusSeconds(15 * 60))
                .activeSince(Instant.now().minusSeconds(15 * 60)).build();
        session.setId(UUID.randomUUID());
    }

    @Test
    void completesSessionAndStoresSubmittedResultsAfterMinimumDuration() {
        List<ExerciseResult> storedResults = new ArrayList<>();
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(itemRepository.findAllByWorkoutPlanIdOrderByDayOfWeekAscItemOrderAsc(any()))
                .thenReturn(List.of(planItem));
        when(resultRepository.saveAll(anyList())).thenAnswer(invocation -> {
            storedResults.addAll(invocation.getArgument(0));
            return storedResults;
        });
        when(sessionRepository.save(any(WorkoutSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(resultRepository.findAllByWorkoutSessionId(session.getId()))
                .thenAnswer(ignored -> storedResults);

        var result = service.complete(user, session.getId(),
                completionRequest());

        assertThat(result.status()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(result.completedAt()).isNotNull();
        assertThat(result.results()).hasSize(1);
        assertThat(result.results().getFirst().completedSets()).isEqualTo(3);
        assertThat(result.results().getFirst().completedRepetitions()).isEqualTo(10);
        assertThat(result.results().getFirst().caloriesBurned()).isPositive();
        assertThat(result.caloriesBurned()).isPositive();
    }

    @Test
    void rejectsCompletionBeforeThePlannedMinimumDuration() {
        session.setStartedAt(Instant.now());
        session.setActiveSince(Instant.now());
        session.setElapsedSeconds(0);
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(itemRepository.findAllByWorkoutPlanIdOrderByDayOfWeekAscItemOrderAsc(any()))
                .thenReturn(List.of(planItem));

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        service.complete(user, session.getId(), completionRequest()))
                .isInstanceOfSatisfying(BadRequestException.class, error ->
                        assertThat(error.getCode()).isEqualTo("WORKOUT_MINIMUM_TIME_NOT_REACHED"));
    }

    @Test
    void rejectsCompletionWhenExerciseResultsAreMissing() {
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(itemRepository.findAllByWorkoutPlanIdOrderByDayOfWeekAscItemOrderAsc(any()))
                .thenReturn(List.of(planItem));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.complete(
                        user, session.getId(),
                        new CompleteWorkoutSessionRequest(DifficultyRating.MODERATE, null, List.of())))
                .isInstanceOfSatisfying(BadRequestException.class, error ->
                        assertThat(error.getCode()).isEqualTo("WORKOUT_RESULTS_INCOMPLETE"));
    }

    @Test
    void rejectsStartingWorkoutOutsideItsScheduledDate() {
        session.setStatus(SessionStatus.SCHEDULED);
        session.setScheduledDate(LocalDate.now(ZoneId.of("Africa/Lagos")).plusDays(1));
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.start(user, session.getId()))
                .isInstanceOfSatisfying(BadRequestException.class, error ->
                        assertThat(error.getCode()).isEqualTo("WORKOUT_NOT_SCHEDULED_TODAY"));
    }

    @Test
    void pausesAndStoresTheCurrentWorkoutPosition() {
        session.setActiveSince(Instant.now().minusSeconds(120));
        List<ExerciseResult> storedResults = new ArrayList<>();
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(itemRepository.findAllByWorkoutPlanIdOrderByDayOfWeekAscItemOrderAsc(any()))
                .thenReturn(List.of(planItem));
        when(resultRepository.findAllByWorkoutSessionId(session.getId()))
                .thenAnswer(ignored -> storedResults);
        when(resultRepository.saveAll(anyList())).thenAnswer(invocation -> {
            storedResults.clear();
            storedResults.addAll(invocation.getArgument(0));
            return storedResults;
        });
        when(sessionRepository.save(any(WorkoutSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.pause(user, session.getId(), new WorkoutProgressRequest(
                0,
                2,
                WorkoutPhase.REST,
                30,
                List.of(new ExerciseCompletionRequest(planItem.getExercise().getId(), 1, 10, false))));

        assertThat(result.activeSince()).isNull();
        assertThat(result.elapsedSeconds()).isGreaterThanOrEqualTo(120);
        assertThat(result.currentExerciseIndex()).isZero();
        assertThat(result.currentSetNumber()).isEqualTo(2);
        assertThat(result.currentPhase()).isEqualTo(WorkoutPhase.REST);
        assertThat(result.phaseRemainingSeconds()).isEqualTo(30);
        assertThat(result.results()).hasSize(1);
        assertThat(result.results().getFirst().completedSets()).isEqualTo(1);
    }

    @Test
    void recordsAnIntentionallySkippedExerciseWithoutRequiringItsSets() {
        List<ExerciseResult> storedResults = new ArrayList<>();
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(itemRepository.findAllByWorkoutPlanIdOrderByDayOfWeekAscItemOrderAsc(any()))
                .thenReturn(List.of(planItem));
        when(resultRepository.saveAll(anyList())).thenAnswer(invocation -> {
            storedResults.addAll(invocation.getArgument(0));
            return storedResults;
        });
        when(resultRepository.findAllByWorkoutSessionId(session.getId()))
                .thenAnswer(ignored -> storedResults);
        when(sessionRepository.save(any(WorkoutSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.complete(user, session.getId(), new CompleteWorkoutSessionRequest(
                DifficultyRating.MODERATE,
                null,
                List.of(new ExerciseCompletionRequest(
                        planItem.getExercise().getId(), 0, 0, true))));

        assertThat(result.status()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(result.results()).singleElement().satisfies(exercise -> {
            assertThat(exercise.skipped()).isTrue();
            assertThat(exercise.completedSets()).isZero();
        });
    }

    @Test
    void rejectsSkippingAWorkoutOutsideItsScheduledDate() {
        session.setScheduledDate(LocalDate.now(ZoneId.of("Africa/Lagos")).minusDays(1));
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        service.skip(user, session.getId(), new SkipWorkoutSessionRequest("Missed workout")))
                .isInstanceOfSatisfying(BadRequestException.class, error ->
                        assertThat(error.getCode()).isEqualTo("WORKOUT_NOT_SCHEDULED_TODAY"));
    }

    private CompleteWorkoutSessionRequest completionRequest() {
        return new CompleteWorkoutSessionRequest(
                DifficultyRating.MODERATE,
                "Felt good",
                List.of(new ExerciseCompletionRequest(
                        planItem.getExercise().getId(),
                        planItem.getSets(),
                        planItem.getTargetRepetitions(),
                        false)));
    }
}
