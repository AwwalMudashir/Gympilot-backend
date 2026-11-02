package com.project.gympilot_backend.media.service;

import com.project.gympilot_backend.common.exception.ResourceNotFoundException;
import com.project.gympilot_backend.common.exception.StorageException;
import com.project.gympilot_backend.media.dto.ImageUploadResponse;
import com.project.gympilot_backend.media.model.MediaAsset;
import com.project.gympilot_backend.media.model.MediaPurpose;
import com.project.gympilot_backend.media.repository.MediaAssetRepository;
import com.project.gympilot_backend.user.model.User;
import com.project.gympilot_backend.user.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageStorageService {
    private final ImageFileValidator imageFileValidator;
    private final CloudinaryGateway cloudinaryGateway;
    private final MediaAssetRepository mediaAssetRepository;
    private final UserRepository userRepository;

    @Transactional
    public ImageUploadResponse upload(User owner, MultipartFile file, MediaPurpose purpose) {
        if (purpose == MediaPurpose.PROFILE_PICTURE) {
            return replaceProfilePicture(owner, file);
        }
        return ImageUploadResponse.from(store(owner, file, purpose));
    }

    @Transactional
    public ImageUploadResponse replaceProfilePicture(User owner, MultipartFile file) {
        MediaAsset previous = mediaAssetRepository
                .findFirstByOwnerIdAndPurposeAndDeletedAtIsNullOrderByCreatedAtDesc(
                        owner.getId(), MediaPurpose.PROFILE_PICTURE)
                .orElse(null);

        MediaAsset uploaded = store(owner, file, MediaPurpose.PROFILE_PICTURE);
        owner.setProfilePictureUrl(uploaded.getSecureUrl());
        userRepository.save(owner);

        if (previous != null && !previous.getId().equals(uploaded.getId())) {
            previous.setDeletedAt(Instant.now());
            mediaAssetRepository.save(previous);
            deleteReplacedRemoteImage(previous);
        }
        return ImageUploadResponse.from(uploaded);
    }

    @Transactional
    public void delete(User owner, UUID mediaId) {
        MediaAsset asset = mediaAssetRepository.findByIdAndOwnerIdAndDeletedAtIsNull(mediaId, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Image was not found"));
        cloudinaryGateway.delete(asset.getPublicId());
        markDeleted(owner, asset);
    }

    @Transactional
    public void deleteProfilePicture(User owner) {
        MediaAsset current = mediaAssetRepository
                .findFirstByOwnerIdAndPurposeAndDeletedAtIsNullOrderByCreatedAtDesc(
                        owner.getId(), MediaPurpose.PROFILE_PICTURE)
                .orElse(null);
        if (current != null) {
            cloudinaryGateway.delete(current.getPublicId());
            markDeleted(owner, current);
        } else if (owner.getProfilePictureUrl() != null) {
            owner.setProfilePictureUrl(null);
            userRepository.save(owner);
        }
    }

    private MediaAsset store(User owner, MultipartFile file, MediaPurpose purpose) {
        byte[] content = imageFileValidator.validateAndRead(file);
        CloudinaryUploadResult result = cloudinaryGateway.upload(content, owner.getId(), purpose);
        MediaAsset asset = MediaAsset.builder()
                .owner(owner)
                .assetId(result.assetId())
                .publicId(result.publicId())
                .secureUrl(result.secureUrl())
                .resourceType(result.resourceType() == null ? "image" : result.resourceType())
                .format(result.format())
                .byteSize(result.byteSize())
                .width(result.width())
                .height(result.height())
                .purpose(purpose)
                .originalFilename(safeFilename(file.getOriginalFilename()))
                .build();
        try {
            return mediaAssetRepository.save(asset);
        } catch (RuntimeException exception) {
            try {
                cloudinaryGateway.delete(result.publicId());
            } catch (StorageException cleanupException) {
                log.warn("Could not remove Cloudinary image after database persistence failure: {}", result.publicId());
            }
            throw exception;
        }
    }

    private void markDeleted(User owner, MediaAsset asset) {
        asset.setDeletedAt(Instant.now());
        mediaAssetRepository.save(asset);
        if (asset.getPurpose() == MediaPurpose.PROFILE_PICTURE
                && asset.getSecureUrl().equals(owner.getProfilePictureUrl())) {
            owner.setProfilePictureUrl(null);
            userRepository.save(owner);
        }
    }

    private void deleteReplacedRemoteImage(MediaAsset asset) {
        try {
            cloudinaryGateway.delete(asset.getPublicId());
        } catch (StorageException exception) {
            log.warn("Replacement succeeded but the previous Cloudinary image could not be deleted: {}",
                    asset.getPublicId());
        }
    }

    private String safeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) return null;
        String normalized = originalFilename.replace('\\', '/');
        String filename = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        if (filename.isBlank()) return null;
        return filename.length() <= 255 ? filename : filename.substring(filename.length() - 255);
    }
}
