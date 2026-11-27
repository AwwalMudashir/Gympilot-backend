package com.project.gympilot_backend.coach.repository;

import com.project.gympilot_backend.coach.model.CoachBrief;
import com.project.gympilot_backend.coach.model.BriefType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoachBriefRepository extends JpaRepository<CoachBrief, UUID> {
    List<CoachBrief> findAllByUserIdOrderByGeneratedAtDesc(UUID userId);

    Optional<CoachBrief> findFirstByUserIdAndBriefTypeAndGeneratedAtBetweenOrderByGeneratedAtDesc(
            UUID userId, BriefType briefType, Instant from, Instant to);

    Optional<CoachBrief> findByIdAndUserId(UUID id, UUID userId);
}
