package com.project.gympilot_backend.coach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.project.gympilot_backend.coach.dto.CoachContext;
import com.project.gympilot_backend.coach.model.BriefType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import tools.jackson.databind.ObjectMapper;

class CoachRecommendationGeneratorTest {
    @Test
    void usesConservativeFallbackWhenOllamaIsUnavailable() {
        @SuppressWarnings("unchecked")
        ObjectProvider<ChatModel> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        CoachRecommendationGenerator generator = new CoachRecommendationGenerator(
                provider, new ObjectMapper(), true);

        CoachContext context = new CoachContext(
                BriefType.DAILY,
                LocalDate.of(2026, 11, 24),
                LocalDate.of(2026, 11, 24),
                new CoachContext.ProfileFacts("strength", "beginner", 3, 30),
                new CoachContext.TodayWorkoutFacts(true, false, "Arms focus", 30, 4),
                new CoachContext.WorkoutFacts(
                        1, 1, 0, 4, BigDecimal.valueOf(180), 64.0, List.of("elbow drift")),
                new CoachContext.WellnessFacts(
                        300, 480, 2, 0, 1, 600, 2000, 500,
                        BigDecimal.valueOf(35), BigDecimal.valueOf(60), BigDecimal.valueOf(15)),
                List.of(),
                List.of("Average sleep: 5h 0m", "Average energy: 2.0/5"));

        var brief = generator.generate(context);

        assertThat(brief.generationSource()).isEqualTo("FALLBACK");
        assertThat(brief.focusToday()).contains("comfortable");
        assertThat(brief.cautions())
                .anyMatch(value -> value.contains("intensity"))
                .anyMatch(value -> value.contains("form scores"));
    }
}
