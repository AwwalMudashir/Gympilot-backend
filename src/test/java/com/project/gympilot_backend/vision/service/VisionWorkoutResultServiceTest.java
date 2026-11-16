package com.project.gympilot_backend.vision.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.project.gympilot_backend.common.exception.BadRequestException;
import com.project.gympilot_backend.user.model.User;
import com.project.gympilot_backend.vision.dto.VisionWorkoutSummaryRequest;
import com.project.gympilot_backend.vision.model.ExerciseResult;
import com.project.gympilot_backend.vision.repository.ExerciseResultRepository;
import com.project.gympilot_backend.workout.model.Exercise;
import com.project.gympilot_backend.workout.model.SessionStatus;
import com.project.gympilot_backend.workout.model.WorkoutPlan;
import com.project.gympilot_backend.workout.model.WorkoutPlanItem;
import com.project.gympilot_backend.workout.model.WorkoutSession;
import com.project.gympilot_backend.workout.repository.WorkoutPlanItemRepository;
import com.project.gympilot_backend.workout.repository.WorkoutSessionRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VisionWorkoutResultServiceTest {
    @Mock WorkoutSessionRepository sessionRepository;
    @Mock WorkoutPlanItemRepository planItemRepository;
    @Mock ExerciseResultRepository resultRepository;

    private VisionWorkoutResultService service;
    private User user;
    private Exercise exercise;
    private WorkoutSession session;
    private WorkoutPlanItem planItem;

    @BeforeEach
    void setUp() {
        service = new VisionWorkoutResultService(
                sessionRepository, planItemRepository, resultRepository, "Africa/Lagos");

        user = User.builder().email("member@example.com").build();
        user.setId(UUID.randomUUID());

        WorkoutPlan plan = WorkoutPlan.builder().user(user).weekStartDate(LocalDate.now()).build();
        plan.setId(UUID.randomUUID());

        exercise = Exercise.builder()
                .code("BODYWEIGHT_SQUAT")
                .name("Bodyweight Squat")
                .cameraSupported(true)
                .active(true)
                .build();
        exercise.setId(UUID.randomUUID());

        planItem = WorkoutPlanItem.builder()
                .workoutPlan(plan)
                .exercise(exercise)
                .dayOfWeek(DayOfWeek.FRIDAY.getValue())
                .sets(3)
                .targetRepetitions(10)
                .restSeconds(45)
                .itemOrder(1)
                .cameraSupported(true)
                .build();

        session = WorkoutSession.builder()
                .user(user)
                .workoutPlan(plan)
                .workoutDay(DayOfWeek.FRIDAY.getValue())
                .scheduledDate(LocalDate.now())
                .status(SessionStatus.IN_PROGRESS)
                .build();
        session.setId(UUID.randomUUID());
    }

    @Test
    void savesOnlyCompactVisionMetricsForAnActiveOwnedSquat() {
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(planItemRepository.findAllByWorkoutPlanIdOrderByDayOfWeekAscItemOrderAsc(any()))
                .thenReturn(List.of(planItem));
        when(resultRepository.findByWorkoutSessionIdAndExerciseId(session.getId(), exercise.getId()))
                .thenReturn(Optional.empty());
        when(resultRepository.save(any(ExerciseResult.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.save(user, request(List.of(
                "insufficient_depth", "EXCESSIVE_FORWARD_LEAN", "insufficient_depth")));

        assertThat(response.completedSets()).isEqualTo(1);
        assertThat(response.completedRepetitions()).isEqualTo(10);
        assertThat(response.averageTempoSeconds()).isEqualByComparingTo("2.45");
        assertThat(response.formScore()).isEqualByComparingTo("82.50");
        assertThat(response.formFlags())
                .containsExactly("INSUFFICIENT_DEPTH", "EXCESSIVE_FORWARD_LEAN");
    }

    @Test
    void rejectsAnUnsupportedFormFlag() {
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(planItemRepository.findAllByWorkoutPlanIdOrderByDayOfWeekAscItemOrderAsc(any()))
                .thenReturn(List.of(planItem));

        assertThatThrownBy(() -> service.save(user, request(List.of("UNKNOWN_FLAG"))))
                .isInstanceOfSatisfying(BadRequestException.class, error ->
                        assertThat(error.getCode()).isEqualTo("VISION_FORM_FLAG_INVALID"));
    }

    private VisionWorkoutSummaryRequest request(List<String> flags) {
        return new VisionWorkoutSummaryRequest(
                session.getId(), exercise.getId(), 1, 10,
                new BigDecimal("2.45"), flags, new BigDecimal("82.50"));
    }
}
