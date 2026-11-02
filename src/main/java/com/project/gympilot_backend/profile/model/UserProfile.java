package com.project.gympilot_backend.profile.model;

import com.project.gympilot_backend.common.model.BaseEntity;
import com.project.gympilot_backend.user.model.User;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_profiles")
public class UserProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private Integer age;

    @Column(name = "height_cm", precision = 5, scale = 2)
    private BigDecimal heightCm;

    @Column(name = "weight_kg", precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_level", length = 30)
    private ExperienceLevel experienceLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "fitness_goal", length = 40)
    private FitnessGoal fitnessGoal;

    @Column(name = "preferred_workout_location", length = 80)
    private String preferredWorkoutLocation;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_profile_equipment", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "equipment", length = 100)
    @Builder.Default
    private Set<String> availableEquipment = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_profile_workout_days", joinColumns = @JoinColumn(name = "profile_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "workout_day", length = 15)
    @Builder.Default
    private Set<DayOfWeek> preferredWorkoutDays = new HashSet<>();

    @Column(name = "days_per_week")
    private Integer daysPerWeek;

    @Column(name = "workout_duration_minutes")
    private Integer workoutDurationMinutes;

    @Column(name = "daily_water_target_ml")
    private Integer dailyWaterTargetMl;

    @Column(name = "preferred_bedtime")
    private LocalTime preferredBedtime;

    @Column(name = "preferred_wake_time")
    private LocalTime preferredWakeTime;

    @Column(name = "sleep_target_minutes")
    private Integer sleepTargetMinutes;

    @Column(name = "waist_cm", precision = 5, scale = 2)
    private BigDecimal waistCm;

    @Column(name = "chest_cm", precision = 5, scale = 2)
    private BigDecimal chestCm;

    @Column(name = "arm_cm", precision = 5, scale = 2)
    private BigDecimal armCm;
}
