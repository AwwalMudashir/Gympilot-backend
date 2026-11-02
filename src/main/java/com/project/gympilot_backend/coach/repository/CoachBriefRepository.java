package com.project.gympilot_backend.coach.repository;

import com.project.gympilot_backend.coach.model.CoachBrief;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoachBriefRepository extends JpaRepository<CoachBrief, UUID> {
    List<CoachBrief> findAllByUserIdOrderByGeneratedAtDesc(UUID userId);
}
