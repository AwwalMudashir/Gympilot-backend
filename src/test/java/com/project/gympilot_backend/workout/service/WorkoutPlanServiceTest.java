package com.project.gympilot_backend.workout.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.project.gympilot_backend.profile.model.ExperienceLevel;
import com.project.gympilot_backend.profile.model.FitnessGoal;
import com.project.gympilot_backend.profile.model.UserProfile;
import com.project.gympilot_backend.profile.repository.UserProfileRepository;
import com.project.gympilot_backend.user.model.User;
import com.project.gympilot_backend.workout.model.Exercise;
import com.project.gympilot_backend.workout.model.WorkoutPlan;
import com.project.gympilot_backend.workout.model.WorkoutPlanItem;
import com.project.gympilot_backend.workout.repository.ExerciseRepository;
import com.project.gympilot_backend.workout.repository.WorkoutPlanItemRepository;
import com.project.gympilot_backend.workout.repository.WorkoutPlanRepository;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkoutPlanServiceTest {
    @Mock WorkoutPlanRepository planRepository;
    @Mock WorkoutPlanItemRepository itemRepository;
    @Mock ExerciseRepository exerciseRepository;
    @Mock UserProfileRepository profileRepository;
    private WorkoutPlanService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new WorkoutPlanService(planRepository, itemRepository, exerciseRepository, profileRepository);
        user = User.builder().email("member@example.com").firstName("Gym").lastName("Pilot").build();
        user.setId(UUID.randomUUID());
    }

    @Test
    void generatesAPlanFromProfileAvailabilityAndEquipment() {
        UserProfile profile = UserProfile.builder()
                .user(user)
                .experienceLevel(ExperienceLevel.BEGINNER)
                .fitnessGoal(FitnessGoal.GENERAL_FITNESS)
                .availableEquipment(Set.of("Dumbbells"))
                .preferredWorkoutDays(Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
                .daysPerWeek(3)
                .workoutDurationMinutes(45)
                .build();
        List<Exercise> exercises = List.of(
                exercise("SQUAT", "Squat", "Lower body", "Bodyweight", true),
                exercise("PUSH_UP", "Push-up", "Upper body", "Bodyweight", false),
                exercise("CURL", "Curl", "Arms", "Dumbbells", true),
                exercise("ROW", "Row", "Back", "Dumbbells", false));
        List<WorkoutPlanItem> storedItems = new ArrayList<>();

        when(planRepository.findFirstByUserIdAndWeekStartDateOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.empty());
        when(profileRepository.findByUserId(user.getId())).thenReturn(Optional.of(profile));
        when(exerciseRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(exercises);
        when(planRepository.save(any(WorkoutPlan.class))).thenAnswer(invocation -> {
            WorkoutPlan plan = invocation.getArgument(0);
            plan.setId(UUID.randomUUID());
            return plan;
        });
        when(itemRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<WorkoutPlanItem> items = invocation.getArgument(0);
            items.forEach(item -> item.setId(UUID.randomUUID()));
            storedItems.addAll(items);
            return items;
        });
        when(itemRepository.findAllByWorkoutPlanIdOrderByDayOfWeekAscItemOrderAsc(any()))
                .thenAnswer(ignored -> storedItems);

        var result = service.generate(user);

        assertThat(result.workoutDays()).isEqualTo(3);
        assertThat(result.totalExercises()).isEqualTo(12);
        assertThat(result.days()).extracting(day -> day.day())
                .containsExactly(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
        assertThat(result.days()).allSatisfy(day -> assertThat(day.exercises()).hasSize(4));
        assertThat(result.days().getFirst().exercises().getFirst().sets()).isEqualTo(2);
    }

    private Exercise exercise(String code, String name, String muscle, String equipment, boolean camera) {
        Exercise exercise = Exercise.builder().code(code).name(name).instructions("Move with control")
                .muscleGroup(muscle).equipment(equipment).cameraSupported(camera).active(true).build();
        exercise.setId(UUID.randomUUID());
        return exercise;
    }
}
