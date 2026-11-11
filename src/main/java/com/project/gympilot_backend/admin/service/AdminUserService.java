package com.project.gympilot_backend.admin.service;

import com.project.gympilot_backend.admin.dto.AdminUserResponse;
import com.project.gympilot_backend.auth.repository.RefreshTokenRepository;
import com.project.gympilot_backend.common.exception.BadRequestException;
import com.project.gympilot_backend.common.exception.ResourceNotFoundException;
import com.project.gympilot_backend.media.model.MediaAsset;
import com.project.gympilot_backend.media.repository.MediaAssetRepository;
import com.project.gympilot_backend.media.service.CloudinaryGateway;
import com.project.gympilot_backend.user.model.User;
import com.project.gympilot_backend.user.model.UserRole;
import com.project.gympilot_backend.user.model.UserStatus;
import com.project.gympilot_backend.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final CloudinaryGateway cloudinaryGateway;

    public AdminUserService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            MediaAssetRepository mediaAssetRepository,
            CloudinaryGateway cloudinaryGateway
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.mediaAssetRepository = mediaAssetRepository;
        this.cloudinaryGateway = cloudinaryGateway;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(AdminUserResponse::from)
                .toList();
    }

    @Transactional
    public AdminUserResponse deactivate(User administrator, UUID userId) {
        User target = requireManageableUser(administrator, userId);
        target.setStatus(UserStatus.INACTIVE);
        target.setLockedUntil(null);
        refreshTokenRepository.revokeAllForUser(target.getId(), Instant.now());
        return AdminUserResponse.from(userRepository.save(target));
    }

    @Transactional
    public AdminUserResponse reactivate(User administrator, UUID userId) {
        User target = requireManageableUser(administrator, userId);
        target.setStatus(UserStatus.ACTIVE);
        target.setFailedLoginAttempts(0);
        target.setLockedUntil(null);
        return AdminUserResponse.from(userRepository.save(target));
    }

    @Transactional
    public void permanentlyDelete(User administrator, UUID userId, String confirmEmail) {
        User target = requireManageableUser(administrator, userId);
        if (confirmEmail == null || !target.getEmail().equalsIgnoreCase(confirmEmail.trim())) {
            throw new BadRequestException(
                    "ACCOUNT_DELETE_CONFIRMATION_INVALID",
                    "The confirmation email does not match the account being deleted");
        }

        List<MediaAsset> activeAssets = mediaAssetRepository.findAllByOwnerIdAndDeletedAtIsNull(target.getId());
        activeAssets.forEach(asset -> cloudinaryGateway.delete(asset.getPublicId()));

        userRepository.delete(target);
        userRepository.flush();
    }

    private User requireManageableUser(User administrator, UUID userId) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User account was not found"));
        if (target.getId().equals(administrator.getId())) {
            throw new BadRequestException(
                    "ADMIN_SELF_MANAGEMENT_NOT_ALLOWED",
                    "Use a different administrator account to manage your own account");
        }
        if (target.getRole() == UserRole.ADMIN) {
            throw new BadRequestException(
                    "ADMIN_ACCOUNT_PROTECTED",
                    "Administrator accounts cannot be changed from this dashboard");
        }
        return target;
    }
}
