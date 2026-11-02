package com.project.gympilot_backend.workout.repository;

import com.project.gympilot_backend.workout.model.WorkoutSession;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, UUID> {
    List<WorkoutSession> findAllByUserIdOrderByScheduledDateDesc(UUID userId);
}
