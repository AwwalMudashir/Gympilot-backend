package com.project.gympilot_backend.profile.service;

import com.project.gympilot_backend.common.exception.ConflictException;
import com.project.gympilot_backend.common.exception.ResourceNotFoundException;
import com.project.gympilot_backend.profile.dto.ProfileRequest;
import com.project.gympilot_backend.profile.dto.ProfileResponse;
import com.project.gympilot_backend.profile.model.UserProfile;
import com.project.gympilot_backend.profile.repository.UserProfileRepository;
import com.project.gympilot_backend.user.model.User;
import java.util.HashSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final UserProfileRepository profileRepository;

    public ProfileService(UserProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Transactional(readOnly = true)
    public ProfileResponse get(User user) {
        return ProfileResponse.from(requireProfile(user));
    }

    @Transactional
    public ProfileResponse create(User user, ProfileRequest request) {
        if (profileRepository.existsByUserId(user.getId())) {
            throw new ConflictException("PROFILE_EXISTS", "A fitness profile already exists");
        }
        UserProfile profile = UserProfile.builder().user(user).build();
        apply(profile, request);
        return ProfileResponse.from(profileRepository.save(profile));
    }

    @Transactional
    public ProfileResponse update(User user, ProfileRequest request) {
        UserProfile profile = requireProfile(user);
        apply(profile, request);
        return ProfileResponse.from(profileRepository.save(profile));
    }

    private UserProfile requireProfile(User user) {
        return profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Fitness profile has not been created"));
    }

    private void apply(UserProfile profile, ProfileRequest request) {
        var selectedWorkoutDays = new HashSet<>(request.preferredWorkoutDays() == null
                ? java.util.Set.of()
                : request.preferredWorkoutDays());

        profile.setAge(request.age());
        profile.setHeightCm(request.heightCm());
        profile.setWeightKg(request.weightKg());
        profile.setExperienceLevel(request.experienceLevel());
        profile.setFitnessGoal(request.fitnessGoal());
        profile.setPreferredWorkoutLocation(request.preferredWorkoutLocation().trim());
        profile.setAvailableEquipment(new HashSet<>(request.availableEquipment() == null ? java.util.Set.of() : request.availableEquipment()));
        profile.setPreferredWorkoutDays(selectedWorkoutDays);
        profile.setDaysPerWeek(selectedWorkoutDays.isEmpty()
                ? request.daysPerWeek()
                : selectedWorkoutDays.size());
        profile.setWorkoutDurationMinutes(request.workoutDurationMinutes());
        profile.setDailyWaterTargetMl(request.dailyWaterTargetMl());
        profile.setPreferredBedtime(request.preferredBedtime());
        profile.setPreferredWakeTime(request.preferredWakeTime());
        profile.setSleepTargetMinutes(request.sleepTargetMinutes());
        profile.setWaistCm(request.waistCm());
        profile.setChestCm(request.chestCm());
        profile.setArmCm(request.armCm());
    }
}
