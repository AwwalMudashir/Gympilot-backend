package com.project.gympilot_backend.workout.service;

import com.project.gympilot_backend.workout.dto.ExerciseResponse;
import com.project.gympilot_backend.workout.repository.ExerciseRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExerciseService {
    private final ExerciseRepository exerciseRepository;

    public ExerciseService(ExerciseRepository exerciseRepository) {
        this.exerciseRepository = exerciseRepository;
    }

    @Transactional(readOnly = true)
    public List<ExerciseResponse> list(Boolean cameraSupported) {
        return exerciseRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .filter(exercise -> cameraSupported == null || exercise.isCameraSupported() == cameraSupported)
                .map(ExerciseResponse::from)
                .toList();
    }
}
