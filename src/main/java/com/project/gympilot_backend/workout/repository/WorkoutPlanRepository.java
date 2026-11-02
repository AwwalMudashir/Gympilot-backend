package com.project.gympilot_backend.workout.repository;

import com.project.gympilot_backend.workout.model.WorkoutPlan;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, UUID> {
    List<WorkoutPlan> findAllByUserIdOrderByWeekStartDateDesc(UUID userId);
}
