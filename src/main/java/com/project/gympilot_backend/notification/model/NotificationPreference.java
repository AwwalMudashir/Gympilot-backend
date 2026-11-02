package com.project.gympilot_backend.notification.model;

import com.project.gympilot_backend.common.model.BaseEntity;
import com.project.gympilot_backend.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "notification_preferences")
public class NotificationPreference extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    @Column(name = "workout_enabled", nullable = false)
    private boolean workoutEnabled;
    @Column(name = "workout_time")
    private LocalTime workoutTime;
    @Column(name = "hydration_enabled", nullable = false)
    private boolean hydrationEnabled;
    @Column(name = "hydration_interval_minutes")
    private Integer hydrationIntervalMinutes;
    @Column(name = "bedtime_enabled", nullable = false)
    private boolean bedtimeEnabled;
    @Column(name = "bedtime_reminder_time")
    private LocalTime bedtimeReminderTime;
    @Column(name = "sleep_checkin_enabled", nullable = false)
    private boolean sleepCheckinEnabled;
    @Column(name = "sleep_checkin_time")
    private LocalTime sleepCheckinTime;
    @Column(name = "all_notifications_enabled", nullable = false)
    private boolean allNotificationsEnabled;
    @Column(name = "device_token", length = 512)
    private String deviceToken;
}
