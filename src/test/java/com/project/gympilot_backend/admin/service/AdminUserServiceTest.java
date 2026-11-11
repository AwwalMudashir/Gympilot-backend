package com.project.gympilot_backend.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.gympilot_backend.auth.repository.RefreshTokenRepository;
import com.project.gympilot_backend.common.exception.BadRequestException;
import com.project.gympilot_backend.media.model.MediaAsset;
import com.project.gympilot_backend.media.repository.MediaAssetRepository;
import com.project.gympilot_backend.media.service.CloudinaryGateway;
import com.project.gympilot_backend.user.model.User;
import com.project.gympilot_backend.user.model.UserRole;
import com.project.gympilot_backend.user.model.UserStatus;
import com.project.gympilot_backend.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private MediaAssetRepository mediaAssetRepository;
    @Mock private CloudinaryGateway cloudinaryGateway;

    private AdminUserService service;
    private User administrator;
    private User member;

    @BeforeEach
    void setUp() {
        service = new AdminUserService(
                userRepository,
                refreshTokenRepository,
                mediaAssetRepository,
                cloudinaryGateway);
        administrator = user(UUID.randomUUID(), "admin@gympilot.test", UserRole.ADMIN);
        member = user(UUID.randomUUID(), "member@gympilot.test", UserRole.USER);
    }

    @Test
    void deactivationRevokesSessionsAndKeepsUserData() {
        when(userRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(userRepository.save(member)).thenReturn(member);

        var response = service.deactivate(administrator, member.getId());

        assertThat(response.status()).isEqualTo(UserStatus.INACTIVE);
        verify(refreshTokenRepository).revokeAllForUser(org.mockito.ArgumentMatchers.eq(member.getId()),
                org.mockito.ArgumentMatchers.any());
        verify(userRepository, never()).delete(member);
    }

    @Test
    void permanentDeletionCleansCloudinaryThenDeletesCascadeRoot() {
        MediaAsset asset = MediaAsset.builder().publicId("gympilot/member/profile").owner(member).build();
        when(userRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(mediaAssetRepository.findAllByOwnerIdAndDeletedAtIsNull(member.getId()))
                .thenReturn(List.of(asset));

        service.permanentlyDelete(administrator, member.getId(), " MEMBER@gympilot.test ");

        verify(cloudinaryGateway).delete("gympilot/member/profile");
        verify(userRepository).delete(member);
        verify(userRepository).flush();
    }

    @Test
    void permanentDeletionRequiresMatchingEmail() {
        when(userRepository.findById(member.getId())).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> service.permanentlyDelete(
                administrator, member.getId(), "someone-else@gympilot.test"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("confirmation email");

        verify(userRepository, never()).delete(member);
        verify(mediaAssetRepository, never()).findAllByOwnerIdAndDeletedAtIsNull(member.getId());
    }

    @Test
    void administratorAccountsAreProtected() {
        User secondAdministrator = user(UUID.randomUUID(), "admin2@gympilot.test", UserRole.ADMIN);
        when(userRepository.findById(secondAdministrator.getId())).thenReturn(Optional.of(secondAdministrator));

        assertThatThrownBy(() -> service.deactivate(administrator, secondAdministrator.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Administrator accounts");
    }

    private User user(UUID id, String email, UserRole role) {
        User user = User.builder()
                .email(email)
                .firstName("Gym")
                .lastName("Pilot")
                .role(role)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();
        user.setId(id);
        return user;
    }
}
