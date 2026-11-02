package com.project.gympilot_backend.media.model;

import com.project.gympilot_backend.common.model.BaseEntity;
import com.project.gympilot_backend.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "media_assets")
public class MediaAsset extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "asset_id", unique = true, length = 255)
    private String assetId;

    @Column(name = "public_id", nullable = false, unique = true, length = 512)
    private String publicId;

    @Column(name = "secure_url", nullable = false, length = 2048)
    private String secureUrl;

    @Column(name = "resource_type", nullable = false, length = 30)
    private String resourceType;

    @Column(length = 30)
    private String format;

    @Column(name = "byte_size", nullable = false)
    private long byteSize;

    private Integer width;
    private Integer height;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MediaPurpose purpose;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
