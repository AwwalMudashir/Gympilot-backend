package com.project.gympilot_backend.coach.dto;

import java.util.List;

public record GeneratedCoachBrief(
        String summary,
        String focusToday,
        List<String> cautions,
        String generationSource
) {
}
