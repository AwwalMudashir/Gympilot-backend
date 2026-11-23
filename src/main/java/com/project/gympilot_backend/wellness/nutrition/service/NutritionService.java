package com.project.gympilot_backend.wellness.nutrition.service;

import com.project.gympilot_backend.common.exception.BadRequestException;
import com.project.gympilot_backend.user.model.User;
import com.project.gympilot_backend.wellness.nutrition.dto.DailyNutritionResponse;
import com.project.gympilot_backend.wellness.nutrition.dto.NutritionLogRequest;
import com.project.gympilot_backend.wellness.nutrition.dto.NutritionLogResponse;
import com.project.gympilot_backend.wellness.nutrition.model.NutritionLog;
import com.project.gympilot_backend.wellness.nutrition.repository.NutritionLogRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NutritionService {
    private final NutritionLogRepository repository;
    private final ZoneId applicationZone;

    public NutritionService(
            NutritionLogRepository repository,
            @Value("${app.time-zone:Africa/Lagos}") String timeZone
    ) {
        this.repository = repository;
        this.applicationZone = ZoneId.of(timeZone);
    }

    @Transactional
    public NutritionLogResponse create(User user, NutritionLogRequest request) {
        Instant loggedAt = request.loggedAt() == null ? Instant.now() : request.loggedAt();
        if (loggedAt.isAfter(Instant.now().plus(Duration.ofMinutes(5)))) {
            throw new BadRequestException("MEAL_TIME_IN_FUTURE", "Meal time cannot be in the future");
        }
        NutritionLog log = NutritionLog.builder()
                .user(user)
                .mealName(request.mealName().trim())
                .calories(request.calories())
                .proteinGrams(request.proteinGrams())
                .carbohydrateGrams(request.carbohydrateGrams())
                .fatGrams(request.fatGrams())
                .fibreGrams(request.fibreGrams())
                .loggedAt(loggedAt)
                .build();
        return NutritionLogResponse.from(repository.save(log));
    }

    @Transactional(readOnly = true)
    public DailyNutritionResponse daily(User user, LocalDate requestedDate) {
        LocalDate date = requestedDate == null ? LocalDate.now(applicationZone) : requestedDate;
        Instant from = date.atStartOfDay(applicationZone).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(applicationZone).toInstant();
        List<NutritionLogResponse> entries = repository
                .findAllByUserIdAndLoggedAtBetweenOrderByLoggedAtAsc(user.getId(), from, to)
                .stream()
                .map(NutritionLogResponse::from)
                .toList();
        return new DailyNutritionResponse(
                date,
                entries.stream().mapToInt(NutritionLogResponse::calories).sum(),
                sum(entries, Macro.PROTEIN),
                sum(entries, Macro.CARBOHYDRATE),
                sum(entries, Macro.FAT),
                sum(entries, Macro.FIBRE),
                entries);
    }

    private BigDecimal sum(List<NutritionLogResponse> entries, Macro macro) {
        return entries.stream()
                .map(entry -> switch (macro) {
                    case PROTEIN -> entry.proteinGrams();
                    case CARBOHYDRATE -> entry.carbohydrateGrams();
                    case FAT -> entry.fatGrams();
                    case FIBRE -> entry.fibreGrams();
                })
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private enum Macro { PROTEIN, CARBOHYDRATE, FAT, FIBRE }
}
