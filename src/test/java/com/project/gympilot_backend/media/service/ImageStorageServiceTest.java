package com.project.gympilot_backend.media.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.gympilot_backend.media.dto.ImageUploadResponse;
import com.project.gympilot_backend.media.model.MediaAsset;
import com.project.gympilot_backend.media.model.MediaPurpose;
import com.project.gympilot_backend.media.repository.MediaAssetRepository;
import com.project.gympilot_backend.user.model.User;
import com.project.gympilot_backend.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ImageStorageServiceTest {
    private CloudinaryGateway cloudinaryGateway;
    private MediaAssetRepository mediaAssetRepository;
    private UserRepository userRepository;
    private ImageStorageService service;

    @BeforeEach
    void setUp() {
        ImageFileValidator validator = mock(ImageFileValidator.class);
        cloudinaryGateway = mock(CloudinaryGateway.class);
        mediaAssetRepository = mock(MediaAssetRepository.class);
        userRepository = mock(UserRepository.class);
        service = new ImageStorageService(validator, cloudinaryGateway, mediaAssetRepository, userRepository);

        when(validator.validateAndRead(any())).thenReturn(new byte[]{1, 2, 3});
        when(cloudinaryGateway.upload(any(), any(), any())).thenReturn(new CloudinaryUploadResult(
                "cloudinary-asset-id",
                "gympilot/user/profile/random-id",
                "https://res.cloudinary.com/demo/image/upload/random-id.jpg",
                "image",
                "jpg",
                321L,
                400,
                400
        ));
        when(mediaAssetRepository.save(any(MediaAsset.class))).thenAnswer(invocation -> {
            MediaAsset asset = invocation.getArgument(0);
            if (asset.getId() == null) asset.setId(UUID.randomUUID());
            if (asset.getCreatedAt() == null) asset.setCreatedAt(Instant.now());
            return asset;
        });
    }

    @Test
    void profileUploadStoresCloudinaryUrlAndUpdatesUser() {
        User user = User.builder().email("user@example.com").build();
        user.setId(UUID.randomUUID());
        when(mediaAssetRepository.findFirstByOwnerIdAndPurposeAndDeletedAtIsNullOrderByCreatedAtDesc(
                user.getId(), MediaPurpose.PROFILE_PICTURE)).thenReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", new byte[]{1});

        ImageUploadResponse response = service.replaceProfilePicture(user, file);

        assertThat(response.url()).startsWith("https://res.cloudinary.com/");
        assertThat(user.getProfilePictureUrl()).isEqualTo(response.url());
        verify(userRepository).save(user);
        verify(mediaAssetRepository).save(any(MediaAsset.class));
    }
}
