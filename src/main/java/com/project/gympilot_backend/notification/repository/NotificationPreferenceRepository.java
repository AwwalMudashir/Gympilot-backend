package com.project.gympilot_backend.notification.repository;

import com.project.gympilot_backend.notification.model.NotificationPreference;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {
    Optional<NotificationPreference> findByUserId(UUID userId);
}
