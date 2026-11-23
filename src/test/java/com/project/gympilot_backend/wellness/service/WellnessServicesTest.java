package com.project.gympilot_backend.wellness.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.project.gympilot_backend.common.exception.BadRequestException;
import com.project.gympilot_backend.profile.model.UserProfile;
import com.project.gympilot_backend.profile.repository.UserProfileRepository;
import com.project.gympilot_backend.user.model.User;
import com.project.gympilot_backend.wellness.hydration.model.WaterLog;
import com.project.gympilot_backend.wellness.hydration.repository.WaterLogRepository;
import com.project.gympilot_backend.wellness.hydration.service.HydrationService;
import com.project.gympilot_backend.wellness.nutrition.model.NutritionLog;
import com.project.gympilot_backend.wellness.nutrition.repository.NutritionLogRepository;
import com.project.gympilot_backend.wellness.nutrition.service.NutritionService;
import com.project.gympilot_backend.wellness.sleep.dto.SleepLogRequest;
import com.project.gympilot_backend.wellness.sleep.model.SleepQuality;
import com.project.gympilot_backend.wellness.sleep.repository.SleepLogRepository;
import com.project.gympilot_backend.wellness.sleep.service.SleepLogService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WellnessServicesTest {
    @Mock SleepLogRepository sleepRepository;
    @Mock WaterLogRepository waterRepository;
    @Mock UserProfileRepository profileRepository;
    @Mock NutritionLogRepository nutritionRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().email("member@example.com").build();
        user.setId(UUID.randomUUID());
    }

    @Test
    void rejectsSleepPeriodLongerThanTwentyFourHours() {
        SleepLogService service = new SleepLogService(sleepRepository, "Africa/Lagos");
        Instant wokeAt = Instant.parse("2026-11-18T06:00:00Z");
        SleepLogRequest request = new SleepLogRequest(
                wokeAt.minusSeconds(25 * 60 * 60),
                wokeAt,
                SleepQuality.GOOD,
                4,
                null);

        assertThatThrownBy(() -> service.create(user, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot exceed 24 hours");
    }

    @Test
    void calculatesHydrationAgainstTheUsersTargetAndCapsProgress() {
        HydrationService service = new HydrationService(
                waterRepository, profileRepository, "Africa/Lagos");
        LocalDate date = LocalDate.of(2026, 11, 18);
        WaterLog first = waterLog(750, Instant.parse("2026-11-18T08:00:00Z"));
        WaterLog second = waterLog(850, Instant.parse("2026-11-18T12:00:00Z"));
        UserProfile profile = UserProfile.builder()
                .user(user)
                .dailyWaterTargetMl(1500)
                .build();

        when(waterRepository.findAllByUserIdAndLoggedAtBetweenOrderByLoggedAtAsc(
                any(), any(), any())).thenReturn(List.of(first, second));
        when(profileRepository.findByUserId(user.getId())).thenReturn(Optional.of(profile));

        var result = service.daily(user, date);

        assertThat(result.totalMl()).isEqualTo(1600);
        assertThat(result.targetMl()).isEqualTo(1500);
        assertThat(result.progressPercent()).isEqualTo(100);
        assertThat(result.entries()).hasSize(2);
    }

    @Test
    void totalsDailyCaloriesAndMacronutrients() {
        NutritionService service = new NutritionService(nutritionRepository, "Africa/Lagos");
        LocalDate date = LocalDate.of(2026, 11, 18);
        NutritionLog breakfast = meal("Oats", 420, "25", "60", "9", "8");
        NutritionLog dinner = meal("Chicken rice", 680, "55", "72", "18", "5");

        when(nutritionRepository.findAllByUserIdAndLoggedAtBetweenOrderByLoggedAtAsc(
                any(), any(), any())).thenReturn(List.of(breakfast, dinner));

        var result = service.daily(user, date);

        assertThat(result.calories()).isEqualTo(1100);
        assertThat(result.proteinGrams()).isEqualByComparingTo("80");
        assertThat(result.carbohydrateGrams()).isEqualByComparingTo("132");
        assertThat(result.fatGrams()).isEqualByComparingTo("27");
        assertThat(result.fibreGrams()).isEqualByComparingTo("13");
    }

    private WaterLog waterLog(int amountMl, Instant loggedAt) {
        WaterLog log = WaterLog.builder()
                .user(user)
                .amountMl(amountMl)
                .loggedAt(loggedAt)
                .build();
        log.setId(UUID.randomUUID());
        return log;
    }

    private NutritionLog meal(
            String name,
            int calories,
            String protein,
            String carbohydrate,
            String fat,
            String fibre
    ) {
        NutritionLog log = NutritionLog.builder()
                .user(user)
                .mealName(name)
                .calories(calories)
                .proteinGrams(new BigDecimal(protein))
                .carbohydrateGrams(new BigDecimal(carbohydrate))
                .fatGrams(new BigDecimal(fat))
                .fibreGrams(new BigDecimal(fibre))
                .loggedAt(Instant.parse("2026-11-18T12:00:00Z"))
                .build();
        log.setId(UUID.randomUUID());
        return log;
    }
}
