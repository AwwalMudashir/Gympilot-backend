package com.project.gympilot_backend.wellness.hydration.repository;

import com.project.gympilot_backend.wellness.hydration.model.WaterLog;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaterLogRepository extends JpaRepository<WaterLog, UUID> {
    List<WaterLog> findAllByUserIdAndLoggedAtBetweenOrderByLoggedAtAsc(UUID userId, Instant from, Instant to);
}
