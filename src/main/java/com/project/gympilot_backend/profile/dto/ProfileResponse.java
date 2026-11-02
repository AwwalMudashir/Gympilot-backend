package com.project.gympilot_backend.profile.dto;

import com.project.gympilot_backend.profile.model.ExperienceLevel;
import com.project.gympilot_backend.profile.model.FitnessGoal;
import com.project.gympilot_backend.profile.model.UserProfile;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

public record ProfileResponse(
        UUID id,
        UUID userId,
        String email,
        String firstName,
        String lastName,
        String profilePictureUrl,
        Integer age,
        BigDecimal heightCm,
        BigDecimal weightKg,
        ExperienceLevel experienceLevel,
        FitnessGoal fitnessGoal,
        String preferredWorkoutLocation,
        Set<String> availableEquipment,
        Set<DayOfWeek> preferredWorkoutDays,
        Integer daysPerWeek,
        Integer workoutDurationMinutes,
        Integer dailyWaterTargetMl,
        LocalTime preferredBedtime,
        LocalTime preferredWakeTime,
        Integer sleepTargetMinutes,
        BigDecimal waistCm,
        BigDecimal chestCm,
        BigDecimal armCm
) {
    public static ProfileResponse from(UserProfile profile) {
        var user = profile.getUser();
        return new ProfileResponse(
                profile.getId(), user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getProfilePictureUrl(), profile.getAge(), profile.getHeightCm(), profile.getWeightKg(),
                profile.getExperienceLevel(), profile.getFitnessGoal(), profile.getPreferredWorkoutLocation(),
                Set.copyOf(profile.getAvailableEquipment()), Set.copyOf(profile.getPreferredWorkoutDays()),
                profile.getDaysPerWeek(), profile.getWorkoutDurationMinutes(), profile.getDailyWaterTargetMl(),
                profile.getPreferredBedtime(), profile.getPreferredWakeTime(), profile.getSleepTargetMinutes(),
                profile.getWaistCm(), profile.getChestCm(), profile.getArmCm()
        );
    }
}
