package com.project.gympilot_backend.workout.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.project.gympilot_backend.user.model.User;
import com.project.gympilot_backend.vision.model.ExerciseResult;
import com.project.gympilot_backend.vision.repository.ExerciseResultRepository;
import com.project.gympilot_backend.workout.model.Exercise;
import com.project.gympilot_backend.workout.model.SessionStatus;
import com.project.gympilot_backend.workout.model.WorkoutSession;
import com.project.gympilot_backend.workout.repository.WorkoutSessionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkoutAnalyticsServiceTest {
    @Mock WorkoutSessionRepository sessionRepository;
    @Mock ExerciseResultRepository resultRepository;

    @Test
    void returnsDailyWeeklyAndExerciseBreakdowns() {
        User user = User.builder().email("member@example.com").build();
        user.setId(UUID.randomUUID());
        LocalDate today = LocalDate.now(ZoneId.of("Africa/Lagos"));
        WorkoutSession session = WorkoutSession.builder()
                .user(user)
                .scheduledDate(today)
                .status(SessionStatus.COMPLETED)
                .caloriesBurned(BigDecimal.valueOf(32.5))
                .build();
        session.setId(UUID.randomUUID());
        Exercise exercise = Exercise.builder().name("Squat").build();
        exercise.setId(UUID.randomUUID());
        ExerciseResult result = ExerciseResult.builder()
                .workoutSession(session)
                .exercise(exercise)
                .completedSets(3)
                .completedRepetitions(10)
                .caloriesBurned(BigDecimal.valueOf(32.5))
                .build();

        when(sessionRepository
                .findAllByUserIdAndScheduledDateBetweenOrderByScheduledDateAscCreatedAtAsc(
                        eq(user.getId()), any(), any()))
                .thenReturn(List.of(session));
        when(resultRepository.findAllByWorkoutSessionIdIn(List.of(session.getId())))
                .thenReturn(List.of(result));

        var analytics = new WorkoutAnalyticsService(
                sessionRepository, resultRepository, "Africa/Lagos")
                .currentWeek(user);

        assertThat(analytics.dailyBreakdown()).hasSize(7);
        assertThat(analytics.todayCalories()).isEqualByComparingTo("32.5");
        assertThat(analytics.weekCalories()).isEqualByComparingTo("32.5");
        assertThat(analytics.completedWorkouts()).isEqualTo(1);
        assertThat(analytics.completedExercises()).isEqualTo(1);
        assertThat(analytics.exerciseBreakdown()).singleElement().satisfies(item -> {
            assertThat(item.exerciseName()).isEqualTo("Squat");
            assertThat(item.caloriesBurned()).isEqualByComparingTo("32.5");
            assertThat(item.completedSets()).isEqualTo(3);
        });
    }
}
