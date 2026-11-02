package com.project.gympilot_backend.media.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.project.gympilot_backend.common.exception.BadRequestException;
import com.project.gympilot_backend.config.CloudinaryProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ImageFileValidatorTest {
    private ImageFileValidator validator;

    @BeforeEach
    void setUp() {
        CloudinaryProperties properties = new CloudinaryProperties();
        properties.setMaxImageBytes(1024);
        validator = new ImageFileValidator(properties);
    }

    @Test
    void acceptsPngWithMatchingSignature() {
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", png);

        assertThat(validator.validateAndRead(file)).isEqualTo(png);
    }

    @Test
    void rejectsFileWhoseDeclaredImageTypeDoesNotMatchItsContent() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "not-an-image.png", "image/png", "plain text".getBytes());

        assertThatThrownBy(() -> validator.validateAndRead(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("File content is not a supported image");
    }
}
