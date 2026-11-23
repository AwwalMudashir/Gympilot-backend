package com.project.gympilot_backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.project.gympilot_backend.common.exception.BadRequestException;
import com.project.gympilot_backend.notification.dto.NotificationPreferenceRequest;
import com.project.gympilot_backend.notification.model.NotificationPreference;
import com.project.gympilot_backend.notification.repository.NotificationPreferenceRepository;
import com.project.gympilot_backend.user.model.User;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceTest {
    @Mock NotificationPreferenceRepository repository;

    private NotificationPreferenceService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new NotificationPreferenceService(repository);
        user = User.builder().email("member@example.com").build();
        user.setId(UUID.randomUUID());
    }

    @Test
    void createsSafeDefaultsForAUserWithoutPreferences() {
        when(repository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(repository.save(any(NotificationPreference.class))).thenAnswer(invocation -> {
            NotificationPreference preference = invocation.getArgument(0);
            preference.setId(UUID.randomUUID());
            return preference;
        });

        var response = service.get(user);

        assertThat(response.allNotificationsEnabled()).isTrue();
        assertThat(response.workoutEnabled()).isTrue();
        assertThat(response.workoutTime()).isEqualTo(LocalTime.of(18, 0));
        assertThat(response.hydrationIntervalMinutes()).isEqualTo(120);
    }

    @Test
    void requiresATimeWhenWorkoutRemindersAreEnabled() {
        NotificationPreferenceRequest request = new NotificationPreferenceRequest(
                true,
                true,
                null,
                false,
                120,
                false,
                LocalTime.of(22, 0),
                false,
                LocalTime.of(7, 0),
                null);

        assertThatThrownBy(() -> service.update(user, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("workout reminders");
    }
}
