package com.project.gympilot_backend.wellness.hydration.service;

import com.project.gympilot_backend.common.exception.BadRequestException;
import com.project.gympilot_backend.profile.repository.UserProfileRepository;
import com.project.gympilot_backend.user.model.User;
import com.project.gympilot_backend.wellness.hydration.dto.DailyHydrationResponse;
import com.project.gympilot_backend.wellness.hydration.dto.WaterLogRequest;
import com.project.gympilot_backend.wellness.hydration.dto.WaterLogResponse;
import com.project.gympilot_backend.wellness.hydration.model.WaterLog;
import com.project.gympilot_backend.wellness.hydration.repository.WaterLogRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HydrationService {
    private static final int DEFAULT_TARGET_ML = 2000;

    private final WaterLogRepository repository;
    private final UserProfileRepository profileRepository;
    private final ZoneId applicationZone;

    public HydrationService(
            WaterLogRepository repository,
            UserProfileRepository profileRepository,
            @Value("${app.time-zone:Africa/Lagos}") String timeZone
    ) {
        this.repository = repository;
        this.profileRepository = profileRepository;
        this.applicationZone = ZoneId.of(timeZone);
    }

    @Transactional
    public WaterLogResponse create(User user, WaterLogRequest request) {
        Instant loggedAt = request.loggedAt() == null ? Instant.now() : request.loggedAt();
        if (loggedAt.isAfter(Instant.now().plus(Duration.ofMinutes(5)))) {
            throw new BadRequestException("WATER_TIME_IN_FUTURE", "Water log time cannot be in the future");
        }
        WaterLog log = WaterLog.builder()
                .user(user)
                .amountMl(request.amountMl())
                .loggedAt(loggedAt)
                .build();
        return WaterLogResponse.from(repository.save(log));
    }

    @Transactional(readOnly = true)
    public DailyHydrationResponse daily(User user, LocalDate requestedDate) {
        LocalDate date = requestedDate == null ? LocalDate.now(applicationZone) : requestedDate;
        Instant from = date.atStartOfDay(applicationZone).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(applicationZone).toInstant();
        List<WaterLogResponse> entries = repository
                .findAllByUserIdAndLoggedAtBetweenOrderByLoggedAtAsc(user.getId(), from, to)
                .stream()
                .map(WaterLogResponse::from)
                .toList();
        int total = entries.stream().mapToInt(WaterLogResponse::amountMl).sum();
        int target = profileRepository.findByUserId(user.getId())
                .map(profile -> profile.getDailyWaterTargetMl())
                .filter(value -> value != null && value > 0)
                .orElse(DEFAULT_TARGET_ML);
        int progress = Math.min(100, (int) Math.round(total * 100.0 / target));
        return new DailyHydrationResponse(date, total, target, progress, entries);
    }
}
