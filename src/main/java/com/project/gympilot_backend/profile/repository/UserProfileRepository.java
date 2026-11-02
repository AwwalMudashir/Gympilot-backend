package com.project.gympilot_backend.profile.repository;

import com.project.gympilot_backend.profile.model.UserProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    @EntityGraph(attributePaths = {"availableEquipment", "preferredWorkoutDays"})
    Optional<UserProfile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
