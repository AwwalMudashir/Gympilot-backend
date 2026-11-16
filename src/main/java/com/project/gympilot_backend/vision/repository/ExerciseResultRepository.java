package com.project.gympilot_backend.vision.repository;

import com.project.gympilot_backend.vision.model.ExerciseResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciseResultRepository extends JpaRepository<ExerciseResult, UUID> {
    List<ExerciseResult> findAllByWorkoutSessionId(UUID workoutSessionId);

    List<ExerciseResult> findAllByWorkoutSessionIdIn(List<UUID> workoutSessionIds);

    Optional<ExerciseResult> findByWorkoutSessionIdAndExerciseId(UUID workoutSessionId, UUID exerciseId);

    void deleteAllByWorkoutSessionId(UUID workoutSessionId);
}
