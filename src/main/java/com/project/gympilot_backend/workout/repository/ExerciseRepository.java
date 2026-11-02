package com.project.gympilot_backend.workout.repository;

import com.project.gympilot_backend.workout.model.Exercise;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {
    Optional<Exercise> findByCodeAndActiveTrue(String code);
}
