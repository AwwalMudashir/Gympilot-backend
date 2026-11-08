package com.project.gympilot_backend.workout.repository;

import com.project.gympilot_backend.workout.model.WorkoutPlan;
import com.project.gympilot_backend.workout.model.PlanStatus;
import java.util.List;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, UUID> {
    List<WorkoutPlan> findAllByUserIdOrderByWeekStartDateDesc(UUID userId);
    Optional<WorkoutPlan> findFirstByUserIdAndWeekStartDateOrderByCreatedAtDesc(UUID userId, LocalDate weekStartDate);
    Optional<WorkoutPlan> findFirstByUserIdAndWeekStartDateAndStatusOrderByCreatedAtDesc(
            UUID userId, LocalDate weekStartDate, PlanStatus status);
}
