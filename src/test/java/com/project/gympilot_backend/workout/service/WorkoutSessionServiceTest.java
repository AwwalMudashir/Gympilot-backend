package com.project.gympilot_backend.workout.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.project.gympilot_backend.user.model.User;
import com.project.gympilot_backend.vision.model.ExerciseResult;
import com.project.gympilot_backend.vision.repository.ExerciseResultRepository;
import com.project.gympilot_backend.workout.dto.CompleteWorkoutSessionRequest;
import com.project.gympilot_backend.workout.model.DifficultyRating;
import com.project.gympilot_backend.workout.model.Exercise;
import com.project.gympilot_backend.workout.model.SessionStatus;
import com.project.gympilot_backend.workout.model.WorkoutPlan;
import com.project.gympilot_backend.workout.model.WorkoutPlanItem;
import com.project.gympilot_backend.workout.model.WorkoutSession;
import com.project.gympilot_backend.workout.repository.WorkoutPlanItemRepository;
import com.project.gympilot_backend.workout.repository.WorkoutPlanRepository;
import com.project.gympilot_backend.workout.repository.WorkoutSessionRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
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
    private WorkoutSessionService service;
    private User user;
    private WorkoutSession session;
    private WorkoutPlanItem planItem;

    @BeforeEach
    void setUp() {
        service = new WorkoutSessionService(
                sessionRepository, planRepository, itemRepository, resultRepository);
        user = User.builder().email("member@example.com").firstName("Gym").lastName("Pilot").build();
        user.setId(UUID.randomUUID());
        WorkoutPlan plan = WorkoutPlan.builder().user(user).weekStartDate(LocalDate.now()).build();
        plan.setId(UUID.randomUUID());
        Exercise exercise = Exercise.builder().name("Squat").muscleGroup("Lower body").build();
        exercise.setId(UUID.randomUUID());
        planItem = WorkoutPlanItem.builder()
                .workoutPlan(plan).exercise(exercise).dayOfWeek(DayOfWeek.MONDAY.getValue())
                .sets(3).targetRepetitions(10).restSeconds(60).itemOrder(1).build();
        session = WorkoutSession.builder()
                .user(user).workoutPlan(plan).workoutDay(DayOfWeek.MONDAY.getValue())
                .scheduledDate(LocalDate.now()).status(SessionStatus.IN_PROGRESS).build();
        session.setId(UUID.randomUUID());
    }

    @Test
    void completesSessionAndStoresPlannedResultsWhenNoOverridesAreSupplied() {
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
                new CompleteWorkoutSessionRequest(DifficultyRating.MODERATE, "Felt good", List.of()));

        assertThat(result.status()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(result.completedAt()).isNotNull();
        assertThat(result.results()).hasSize(1);
        assertThat(result.results().getFirst().completedSets()).isEqualTo(3);
        assertThat(result.results().getFirst().completedRepetitions()).isEqualTo(10);
    }
}
