package com.project.gympilot_backend.notification.service;

import com.project.gympilot_backend.common.exception.BadRequestException;
import com.project.gympilot_backend.notification.dto.NotificationPreferenceRequest;
import com.project.gympilot_backend.notification.dto.NotificationPreferenceResponse;
import com.project.gympilot_backend.notification.model.NotificationPreference;
import com.project.gympilot_backend.notification.repository.NotificationPreferenceRepository;
import com.project.gympilot_backend.user.model.User;
import java.time.LocalTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationPreferenceService {
    private static final LocalTime DEFAULT_WORKOUT_TIME = LocalTime.of(18, 0);
    private static final LocalTime DEFAULT_BEDTIME_TIME = LocalTime.of(22, 0);
    private static final LocalTime DEFAULT_SLEEP_CHECKIN_TIME = LocalTime.of(7, 0);
    private static final int DEFAULT_HYDRATION_INTERVAL_MINUTES = 120;

    private final NotificationPreferenceRepository repository;

    public NotificationPreferenceService(NotificationPreferenceRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public NotificationPreferenceResponse get(User user) {
        NotificationPreference preference = repository.findByUserId(user.getId())
                .orElseGet(() -> repository.save(defaultPreference(user)));
        return NotificationPreferenceResponse.from(preference);
    }

    @Transactional
    public NotificationPreferenceResponse update(User user, NotificationPreferenceRequest request) {
        validateEnabledSettings(request);
        NotificationPreference preference = repository.findByUserId(user.getId())
                .orElseGet(() -> defaultPreference(user));
        preference.setAllNotificationsEnabled(request.allNotificationsEnabled());
        preference.setWorkoutEnabled(request.workoutEnabled());
        preference.setWorkoutTime(request.workoutTime());
        preference.setHydrationEnabled(request.hydrationEnabled());
        preference.setHydrationIntervalMinutes(request.hydrationIntervalMinutes());
        preference.setBedtimeEnabled(request.bedtimeEnabled());
        preference.setBedtimeReminderTime(request.bedtimeReminderTime());
        preference.setSleepCheckinEnabled(request.sleepCheckinEnabled());
        preference.setSleepCheckinTime(request.sleepCheckinTime());
        if (request.deviceToken() != null) {
            preference.setDeviceToken(normalizeToken(request.deviceToken()));
        }
        return NotificationPreferenceResponse.from(repository.save(preference));
    }

    private void validateEnabledSettings(NotificationPreferenceRequest request) {
        if (request.workoutEnabled() && request.workoutTime() == null) {
            throw new BadRequestException(
                    "WORKOUT_REMINDER_TIME_REQUIRED",
                    "Choose a time for workout reminders");
        }
        if (request.hydrationEnabled() && request.hydrationIntervalMinutes() == null) {
            throw new BadRequestException(
                    "HYDRATION_INTERVAL_REQUIRED",
                    "Choose an interval for hydration reminders");
        }
        if (request.bedtimeEnabled() && request.bedtimeReminderTime() == null) {
            throw new BadRequestException(
                    "BEDTIME_REMINDER_TIME_REQUIRED",
                    "Choose a time for bedtime reminders");
        }
        if (request.sleepCheckinEnabled() && request.sleepCheckinTime() == null) {
            throw new BadRequestException(
                    "SLEEP_CHECKIN_TIME_REQUIRED",
                    "Choose a time for sleep check-in reminders");
        }
    }

    private NotificationPreference defaultPreference(User user) {
        return NotificationPreference.builder()
                .user(user)
                .allNotificationsEnabled(true)
                .workoutEnabled(true)
                .workoutTime(DEFAULT_WORKOUT_TIME)
                .hydrationEnabled(true)
                .hydrationIntervalMinutes(DEFAULT_HYDRATION_INTERVAL_MINUTES)
                .bedtimeEnabled(false)
                .bedtimeReminderTime(DEFAULT_BEDTIME_TIME)
                .sleepCheckinEnabled(false)
                .sleepCheckinTime(DEFAULT_SLEEP_CHECKIN_TIME)
                .build();
    }

    private String normalizeToken(String token) {
        return token == null || token.isBlank() ? null : token.trim();
    }
}
