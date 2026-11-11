package com.project.gympilot_backend.media.repository;

import com.project.gympilot_backend.media.model.MediaAsset;
import com.project.gympilot_backend.media.model.MediaPurpose;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {
    Optional<MediaAsset> findByIdAndOwnerIdAndDeletedAtIsNull(UUID id, UUID ownerId);

    Optional<MediaAsset> findFirstByOwnerIdAndPurposeAndDeletedAtIsNullOrderByCreatedAtDesc(
            UUID ownerId, MediaPurpose purpose);

    List<MediaAsset> findAllByOwnerIdAndDeletedAtIsNull(UUID ownerId);
}
