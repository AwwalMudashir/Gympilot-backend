package com.project.gympilot_backend.notification.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

public record NotificationPreferenceRequest(
        @NotNull Boolean allNotificationsEnabled,
        @NotNull Boolean workoutEnabled,
        LocalTime workoutTime,
        @NotNull Boolean hydrationEnabled,
        @Min(30) @Max(480) Integer hydrationIntervalMinutes,
        @NotNull Boolean bedtimeEnabled,
        LocalTime bedtimeReminderTime,
        @NotNull Boolean sleepCheckinEnabled,
        LocalTime sleepCheckinTime,
        @Size(max = 512) String deviceToken
) {
}
