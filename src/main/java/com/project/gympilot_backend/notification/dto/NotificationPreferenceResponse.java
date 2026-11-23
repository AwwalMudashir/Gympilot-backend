package com.project.gympilot_backend.notification.dto;

import com.project.gympilot_backend.notification.model.NotificationPreference;
import java.time.LocalTime;
import java.util.UUID;

public record NotificationPreferenceResponse(
        UUID id,
        boolean allNotificationsEnabled,
        boolean workoutEnabled,
        LocalTime workoutTime,
        boolean hydrationEnabled,
        Integer hydrationIntervalMinutes,
        boolean bedtimeEnabled,
        LocalTime bedtimeReminderTime,
        boolean sleepCheckinEnabled,
        LocalTime sleepCheckinTime
) {
    public static NotificationPreferenceResponse from(NotificationPreference preference) {
        return new NotificationPreferenceResponse(
                preference.getId(),
                preference.isAllNotificationsEnabled(),
                preference.isWorkoutEnabled(),
                preference.getWorkoutTime(),
                preference.isHydrationEnabled(),
                preference.getHydrationIntervalMinutes(),
                preference.isBedtimeEnabled(),
                preference.getBedtimeReminderTime(),
                preference.isSleepCheckinEnabled(),
                preference.getSleepCheckinTime());
    }
}
