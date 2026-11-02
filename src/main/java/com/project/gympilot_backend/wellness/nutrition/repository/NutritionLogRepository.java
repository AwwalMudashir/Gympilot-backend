package com.project.gympilot_backend.wellness.nutrition.repository;

import com.project.gympilot_backend.wellness.nutrition.model.NutritionLog;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NutritionLogRepository extends JpaRepository<NutritionLog, UUID> {
    List<NutritionLog> findAllByUserIdAndLoggedAtBetweenOrderByLoggedAtAsc(UUID userId, Instant from, Instant to);
}
