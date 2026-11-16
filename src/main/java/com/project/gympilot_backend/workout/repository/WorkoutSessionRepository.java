package com.project.gympilot_backend.workout.repository;

import com.project.gympilot_backend.workout.model.WorkoutSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, UUID> {
    List<WorkoutSession> findAllByUserIdOrderByScheduledDateDescCreatedAtDesc(UUID userId);
    Optional<WorkoutSession> findByUserIdAndWorkoutPlanIdAndWorkoutDay(
            UUID userId, UUID workoutPlanId, Integer workoutDay);

    List<WorkoutSession> findAllByUserIdAndScheduledDateBetweenOrderByScheduledDateAscCreatedAtAsc(
            UUID userId, LocalDate startDate, LocalDate endDate);
}
