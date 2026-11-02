package com.project.gympilot_backend.workout.repository;

import com.project.gympilot_backend.workout.model.WorkoutPlanItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutPlanItemRepository extends JpaRepository<WorkoutPlanItem, UUID> {
    List<WorkoutPlanItem> findAllByWorkoutPlanIdOrderByDayOfWeekAscItemOrderAsc(UUID workoutPlanId);
}
