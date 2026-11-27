package com.project.gympilot_backend.coach.dto;

import com.project.gympilot_backend.coach.model.CoachFeedback;
import jakarta.validation.constraints.NotNull;

public record CoachFeedbackRequest(@NotNull CoachFeedback feedback) {
}
