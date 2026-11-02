package com.project.gympilot_backend.profile.dto;

import com.project.gympilot_backend.profile.model.ExperienceLevel;
import com.project.gympilot_backend.profile.model.FitnessGoal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

public record ProfileRequest(
        @NotNull @Min(13) @Max(120) Integer age,
        @NotNull @DecimalMin("80.0") @DecimalMax("250.0") BigDecimal heightCm,
        @NotNull @DecimalMin("25.0") @DecimalMax("400.0") BigDecimal weightKg,
        @NotNull ExperienceLevel experienceLevel,
        @NotNull FitnessGoal fitnessGoal,
        @NotBlank @Size(max = 80) String preferredWorkoutLocation,
        @Size(max = 30) Set<@NotBlank @Size(max = 100) String> availableEquipment,
        @Size(max = 7) Set<@Valid DayOfWeek> preferredWorkoutDays,
        @NotNull @Min(1) @Max(7) Integer daysPerWeek,
        @NotNull @Min(10) @Max(300) Integer workoutDurationMinutes,
        @Min(250) @Max(10000) Integer dailyWaterTargetMl,
        LocalTime preferredBedtime,
        LocalTime preferredWakeTime,
        @Min(180) @Max(900) Integer sleepTargetMinutes,
        @DecimalMin("20.0") @DecimalMax("250.0") BigDecimal waistCm,
        @DecimalMin("20.0") @DecimalMax("250.0") BigDecimal chestCm,
        @DecimalMin("10.0") @DecimalMax("100.0") BigDecimal armCm
) {
}
