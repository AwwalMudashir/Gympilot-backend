package com.project.gympilot_backend.wellness.sleep.service;

import com.project.gympilot_backend.common.exception.BadRequestException;
import com.project.gympilot_backend.user.model.User;
import com.project.gympilot_backend.wellness.sleep.dto.SleepLogRequest;
import com.project.gympilot_backend.wellness.sleep.dto.SleepLogResponse;
import com.project.gympilot_backend.wellness.sleep.model.SleepLog;
import com.project.gympilot_backend.wellness.sleep.repository.SleepLogRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SleepLogService {
    private final SleepLogRepository repository;
    private final ZoneId applicationZone;

    public SleepLogService(
            SleepLogRepository repository,
            @Value("${app.time-zone:Africa/Lagos}") String timeZone
    ) {
        this.repository = repository;
        this.applicationZone = ZoneId.of(timeZone);
    }

    @Transactional
    public SleepLogResponse create(User user, SleepLogRequest request) {
        Duration duration = Duration.between(request.sleptAt(), request.wokeAt());
        if (duration.isNegative() || duration.isZero() || duration.toHours() > 24) {
            throw new BadRequestException(
                    "SLEEP_PERIOD_INVALID",
                    "Wake time must be after sleep time and the sleep period cannot exceed 24 hours");
        }
        if (request.wokeAt().isAfter(Instant.now().plus(Duration.ofMinutes(5)))) {
            throw new BadRequestException("SLEEP_TIME_IN_FUTURE", "Wake time cannot be in the future");
        }

        SleepLog log = SleepLog.builder()
                .user(user)
                .sleptAt(request.sleptAt())
                .wokeAt(request.wokeAt())
                .quality(request.quality())
                .energyLevel(request.energyLevel())
                .notes(normalizeNotes(request.notes()))
                .build();
        return SleepLogResponse.from(repository.save(log));
    }

    @Transactional(readOnly = true)
    public List<SleepLogResponse> list(User user, LocalDate fromDate, LocalDate toDate) {
        LocalDate today = LocalDate.now(applicationZone);
        LocalDate from = fromDate == null ? today.minusDays(6) : fromDate;
        LocalDate to = toDate == null ? today : toDate;
        validateRange(from, to);

        Instant fromInstant = from.atStartOfDay(applicationZone).toInstant();
        Instant toExclusive = to.plusDays(1).atStartOfDay(applicationZone).toInstant();
        return repository.findAllByUserIdAndWokeAtBetweenOrderByWokeAtAsc(
                        user.getId(), fromInstant, toExclusive)
                .stream()
                .map(SleepLogResponse::from)
                .toList();
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (to.isBefore(from) || from.plusDays(30).isBefore(to)) {
            throw new BadRequestException(
                    "WELLNESS_DATE_RANGE_INVALID", "Choose a date range of no more than 31 days");
        }
    }

    private String normalizeNotes(String notes) {
        return notes == null || notes.isBlank() ? null : notes.trim();
    }
}
