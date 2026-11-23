package com.project.gympilot_backend.wellness.progress.service;

import com.project.gympilot_backend.user.model.User;
import com.project.gympilot_backend.wellness.hydration.dto.DailyHydrationResponse;
import com.project.gympilot_backend.wellness.hydration.service.HydrationService;
import com.project.gympilot_backend.wellness.nutrition.dto.DailyNutritionResponse;
import com.project.gympilot_backend.wellness.nutrition.service.NutritionService;
import com.project.gympilot_backend.wellness.progress.dto.WellnessDayResponse;
import com.project.gympilot_backend.wellness.progress.dto.WellnessProgressResponse;
import com.project.gympilot_backend.wellness.sleep.dto.SleepLogResponse;
import com.project.gympilot_backend.wellness.sleep.service.SleepLogService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WellnessProgressService {
    private final SleepLogService sleepService;
    private final HydrationService hydrationService;
    private final NutritionService nutritionService;
    private final ZoneId applicationZone;

    public WellnessProgressService(
            SleepLogService sleepService,
            HydrationService hydrationService,
            NutritionService nutritionService,
            @Value("${app.time-zone:Africa/Lagos}") String timeZone
    ) {
        this.sleepService = sleepService;
        this.hydrationService = hydrationService;
        this.nutritionService = nutritionService;
        this.applicationZone = ZoneId.of(timeZone);
    }

    @Transactional(readOnly = true)
    public WellnessProgressResponse progress(User user, int requestedDays) {
        int days = Math.max(1, Math.min(31, requestedDays));
        LocalDate to = LocalDate.now(applicationZone);
        LocalDate from = to.minusDays(days - 1L);
        List<SleepLogResponse> sleepLogs = sleepService.list(user, from, to);
        List<WellnessDayResponse> daily = new ArrayList<>();

        for (int offset = 0; offset < days; offset++) {
            LocalDate date = from.plusDays(offset);
            List<SleepLogResponse> sleepsForDay = sleepLogs.stream()
                    .filter(log -> log.wokeAt().atZone(applicationZone).toLocalDate().equals(date))
                    .toList();
            long sleepMinutes = sleepsForDay.stream()
                    .mapToLong(SleepLogResponse::durationMinutes)
                    .sum();
            SleepLogResponse latestSleep = sleepsForDay.isEmpty()
                    ? null : sleepsForDay.get(sleepsForDay.size() - 1);
            DailyHydrationResponse hydration = hydrationService.daily(user, date);
            DailyNutritionResponse nutrition = nutritionService.daily(user, date);

            daily.add(new WellnessDayResponse(
                    date,
                    sleepMinutes,
                    latestSleep == null ? null : latestSleep.quality(),
                    latestSleep == null ? null : latestSleep.energyLevel(),
                    hydration.totalMl(),
                    hydration.targetMl(),
                    nutrition.calories(),
                    nutrition.proteinGrams(),
                    nutrition.carbohydrateGrams(),
                    nutrition.fatGrams()));
        }

        List<WellnessDayResponse> daysWithSleep = daily.stream()
                .filter(day -> day.sleepMinutes() > 0)
                .toList();
        long averageSleep = daysWithSleep.isEmpty() ? 0 : Math.round(
                daysWithSleep.stream().mapToLong(WellnessDayResponse::sleepMinutes).average().orElse(0));
        double averageEnergy = daily.stream()
                .filter(day -> day.energyLevel() != null)
                .mapToInt(WellnessDayResponse::energyLevel)
                .average()
                .orElse(0);
        averageEnergy = BigDecimal.valueOf(averageEnergy)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();

        return new WellnessProgressResponse(
                from,
                to,
                averageSleep,
                averageEnergy,
                (int) daily.stream().filter(day -> day.waterMl() >= day.waterTargetMl()).count(),
                daily.stream().mapToInt(WellnessDayResponse::nutritionCalories).sum(),
                List.copyOf(daily));
    }
}
