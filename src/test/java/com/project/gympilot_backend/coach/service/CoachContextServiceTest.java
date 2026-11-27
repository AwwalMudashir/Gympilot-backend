package com.project.gympilot_backend.coach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.gympilot_backend.coach.model.BriefType;
import com.project.gympilot_backend.profile.model.ExperienceLevel;
import com.project.gympilot_backend.profile.model.FitnessGoal;
import com.project.gympilot_backend.profile.model.UserProfile;
import com.project.gympilot_backend.profile.repository.UserProfileRepository;
import com.project.gympilot_backend.user.model.User;
import com.project.gympilot_backend.vision.model.ExerciseResult;
import com.project.gympilot_backend.vision.repository.ExerciseResultRepository;
import com.project.gympilot_backend.wellness.progress.dto.WellnessDayResponse;
import com.project.gympilot_backend.wellness.progress.dto.WellnessProgressResponse;
import com.project.gympilot_backend.wellness.progress.service.WellnessProgressService;
import com.project.gympilot_backend.wellness.sleep.model.SleepQuality;
import com.project.gympilot_backend.workout.dto.TodayWorkoutResponse;
import com.project.gympilot_backend.workout.dto.WorkoutDayResponse;
import com.project.gympilot_backend.workout.model.SessionStatus;
import com.project.gympilot_backend.workout.model.WorkoutSession;
import com.project.gympilot_backend.workout.repository.WorkoutSessionRepository;
import com.project.gympilot_backend.workout.service.WorkoutPlanService;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CoachContextServiceTest {
    @Test
    void buildsTraceableContextOnlyFromTheAuthenticatedUsersRecords() {
        UserProfileRepository profileRepository = mock(UserProfileRepository.class);
        WorkoutSessionRepository sessionRepository = mock(WorkoutSessionRepository.class);
        ExerciseResultRepository resultRepository = mock(ExerciseResultRepository.class);
        WorkoutPlanService planService = mock(WorkoutPlanService.class);
        WellnessProgressService wellnessService = mock(WellnessProgressService.class);
        User user = User.builder().email("member@example.com").build();
        user.setId(UUID.randomUUID());
        UserProfile profile = UserProfile.builder()
                .user(user)
                .fitnessGoal(FitnessGoal.STRENGTH)
                .experienceLevel(ExperienceLevel.BEGINNER)
                .daysPerWeek(3)
                .workoutDurationMinutes(30)
                .dailyWaterTargetMl(2000)
                .sleepTargetMinutes(480)
                .build();
        WorkoutSession session = WorkoutSession.builder()
                .user(user)
                .status(SessionStatus.COMPLETED)
                .scheduledDate(LocalDate.now())
                .caloriesBurned(BigDecimal.valueOf(175))
                .build();
        session.setId(UUID.randomUUID());
        ExerciseResult result = ExerciseResult.builder()
                .workoutSession(session)
                .completedSets(3)
                .formScore(BigDecimal.valueOf(82))
                .formWarnings("ELBOW_DRIFT")
                .build();
        WellnessDayResponse wellnessDay = new WellnessDayResponse(
                LocalDate.now(), 420, SleepQuality.GOOD, 4,
                1500, 2000, 1850,
                BigDecimal.valueOf(110), BigDecimal.valueOf(190), BigDecimal.valueOf(55));

        when(profileRepository.findByUserId(user.getId())).thenReturn(Optional.of(profile));
        when(sessionRepository.findAllByUserIdAndScheduledDateBetweenOrderByScheduledDateAscCreatedAtAsc(
                eq(user.getId()), any(), any())).thenReturn(List.of(session));
        when(resultRepository.findAllByWorkoutSessionIdIn(List.of(session.getId())))
                .thenReturn(List.of(result));
        when(planService.today(user)).thenReturn(new TodayWorkoutResponse(
                UUID.randomUUID(), LocalDate.now(), false,
                new WorkoutDayResponse(
                        DayOfWeek.MONDAY, "Strength focus", 30,
                        BigDecimal.valueOf(220), List.of())));
        when(wellnessService.progress(user, 1)).thenReturn(new WellnessProgressResponse(
                LocalDate.now(), LocalDate.now(), 420, 4, 0, 1850, List.of(wellnessDay)));
        CoachContextService service = new CoachContextService(
                profileRepository,
                sessionRepository,
                resultRepository,
                planService,
                wellnessService,
                "Africa/Lagos");

        var context = service.build(user, BriefType.DAILY);

        assertThat(context.profile().fitnessGoal()).isEqualTo("strength");
        assertThat(context.workouts().completedWorkouts()).isEqualTo(1);
        assertThat(context.workouts().averageFormScore()).isEqualTo(82.0);
        assertThat(context.workouts().formFlags()).containsExactly("elbow drift");
        assertThat(context.evidence())
                .contains("Average sleep: 7h 0m", "Hydration today: 1500/2000 ml");
        verify(profileRepository).findByUserId(user.getId());
        verify(sessionRepository)
                .findAllByUserIdAndScheduledDateBetweenOrderByScheduledDateAscCreatedAtAsc(
                        eq(user.getId()), any(), any());
    }
}
