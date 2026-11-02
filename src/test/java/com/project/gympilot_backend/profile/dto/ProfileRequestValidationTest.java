package com.project.gympilot_backend.profile.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.project.gympilot_backend.profile.model.ExperienceLevel;
import com.project.gympilot_backend.profile.model.FitnessGoal;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ProfileRequestValidationTest {
    private static jakarta.validation.ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void rejectsUnsafeOrUnusableProfileValues() {
        ProfileRequest request = new ProfileRequest(
                12,
                new BigDecimal("60"),
                new BigDecimal("500"),
                ExperienceLevel.BEGINNER,
                FitnessGoal.GENERAL_FITNESS,
                " ",
                Set.of("Dumbbells"),
                Set.of(DayOfWeek.MONDAY),
                0,
                5,
                100,
                null,
                null,
                1000,
                null,
                null,
                null
        );

        Set<String> invalidFields = validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());

        assertThat(invalidFields).contains(
                "age", "heightCm", "weightKg", "preferredWorkoutLocation",
                "daysPerWeek", "workoutDurationMinutes", "dailyWaterTargetMl", "sleepTargetMinutes");
    }
}
