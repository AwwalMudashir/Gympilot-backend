package com.project.gympilot_backend.coach.dto;

import com.project.gympilot_backend.coach.model.BriefType;
import com.project.gympilot_backend.coach.model.CoachFeedback;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CoachBriefResponse(
        UUID id,
        BriefType briefType,
        String summary,
        String focusToday,
        List<String> cautions,
        List<String> evidence,
        List<String> missingData,
        String generationSource,
        CoachFeedback feedback,
        Instant feedbackAt,
        Instant generatedAt
) {
}
