package com.project.gympilot_backend.wellness.hydration.dto;

import com.project.gympilot_backend.wellness.hydration.model.WaterLog;
import java.time.Instant;
import java.util.UUID;

public record WaterLogResponse(UUID id, int amountMl, Instant loggedAt) {
    public static WaterLogResponse from(WaterLog log) {
        return new WaterLogResponse(log.getId(), log.getAmountMl(), log.getLoggedAt());
    }
}
