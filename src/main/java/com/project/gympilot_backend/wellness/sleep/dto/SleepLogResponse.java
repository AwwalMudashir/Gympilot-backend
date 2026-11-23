package com.project.gympilot_backend.wellness.sleep.dto;

import com.project.gympilot_backend.wellness.sleep.model.SleepLog;
import com.project.gympilot_backend.wellness.sleep.model.SleepQuality;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public record SleepLogResponse(
        UUID id,
        Instant sleptAt,
        Instant wokeAt,
        long durationMinutes,
        SleepQuality quality,
        int energyLevel,
        String notes
) {
    public static SleepLogResponse from(SleepLog log) {
        return new SleepLogResponse(
                log.getId(),
                log.getSleptAt(),
                log.getWokeAt(),
                Duration.between(log.getSleptAt(), log.getWokeAt()).toMinutes(),
                log.getQuality(),
                log.getEnergyLevel(),
                log.getNotes());
    }
}
