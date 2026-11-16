package com.project.gympilot_backend.workout.service;

import com.project.gympilot_backend.profile.model.UserProfile;
import com.project.gympilot_backend.profile.repository.UserProfileRepository;
import com.project.gympilot_backend.user.model.User;
import com.project.gympilot_backend.workout.dto.ExerciseResponse;
import com.project.gympilot_backend.workout.repository.ExerciseRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExerciseService {
    private final ExerciseRepository exerciseRepository;
    private final UserProfileRepository profileRepository;

    public ExerciseService(
            ExerciseRepository exerciseRepository,
            UserProfileRepository profileRepository
    ) {
        this.exerciseRepository = exerciseRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional(readOnly = true)
    public List<ExerciseResponse> list(User user, Boolean cameraSupported) {
        BigDecimal weightKg = profileRepository.findByUserId(user.getId())
                .map(UserProfile::getWeightKg)
                .orElse(BigDecimal.valueOf(70));
        return exerciseRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .filter(exercise -> cameraSupported == null || exercise.isCameraSupported() == cameraSupported)
                .map(exercise -> ExerciseResponse.from(exercise, weightKg))
                .toList();
    }
}
