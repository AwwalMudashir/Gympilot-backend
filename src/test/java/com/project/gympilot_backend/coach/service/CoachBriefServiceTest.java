package com.project.gympilot_backend.coach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.gympilot_backend.coach.dto.CoachContext;
import com.project.gympilot_backend.coach.dto.GeneratedCoachBrief;
import com.project.gympilot_backend.coach.model.BriefType;
import com.project.gympilot_backend.coach.model.CoachBrief;
import com.project.gympilot_backend.coach.model.CoachFeedback;
import com.project.gympilot_backend.coach.repository.CoachBriefRepository;
import com.project.gympilot_backend.user.model.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class CoachBriefServiceTest {
    @Test
    void storesGenerationSourceAndTraceableEvidenceWithTheBrief() {
        CoachBriefRepository repository = mock(CoachBriefRepository.class);
        CoachContextService contextService = mock(CoachContextService.class);
        CoachRecommendationGenerator generator = mock(CoachRecommendationGenerator.class);
        User user = User.builder().email("member@example.com").build();
        user.setId(UUID.randomUUID());
        CoachContext context = context();

        when(contextService.build(user, BriefType.DAILY)).thenReturn(context);
        when(generator.generate(context)).thenReturn(new GeneratedCoachBrief(
                "Recovery is below target.",
                "Keep today's effort comfortable.",
                List.of("Do not increase intensity today."),
                "FALLBACK"));
        when(repository.save(any(CoachBrief.class))).thenAnswer(invocation -> {
            CoachBrief brief = invocation.getArgument(0);
            brief.setId(UUID.randomUUID());
            return brief;
        });
        CoachBriefService service = new CoachBriefService(
                repository, contextService, generator, new ObjectMapper(), "Africa/Lagos");

        var response = service.daily(user, true);

        assertThat(response.generationSource()).isEqualTo("FALLBACK");
        assertThat(response.evidence()).containsExactly(
                "Average sleep: 5h 0m", "Hydration today: 600/2000 ml");
        assertThat(response.cautions()).containsExactly("Do not increase intensity today.");
        verify(repository).save(any(CoachBrief.class));
    }

    @Test
    void storesFeedbackOnlyForTheAuthenticatedUsersBrief() {
        CoachBriefRepository repository = mock(CoachBriefRepository.class);
        User user = User.builder().email("member@example.com").build();
        user.setId(UUID.randomUUID());
        CoachBrief brief = CoachBrief.builder()
                .user(user)
                .briefType(BriefType.DAILY)
                .summary("Keep today's session comfortable.")
                .cautions("[]")
                .generatedAt(java.time.Instant.now())
                .build();
        brief.setId(UUID.randomUUID());
        when(repository.findByIdAndUserId(brief.getId(), user.getId()))
                .thenReturn(Optional.of(brief));
        when(repository.save(brief)).thenReturn(brief);
        CoachBriefService service = new CoachBriefService(
                repository,
                mock(CoachContextService.class),
                mock(CoachRecommendationGenerator.class),
                new ObjectMapper(),
                "Africa/Lagos");

        var response = service.recordFeedback(user, brief.getId(), CoachFeedback.HELPFUL);

        assertThat(response.feedback()).isEqualTo(CoachFeedback.HELPFUL);
        assertThat(response.feedbackAt()).isNotNull();
        verify(repository).findByIdAndUserId(brief.getId(), user.getId());
        verify(repository).save(brief);
    }

    private CoachContext context() {
        return new CoachContext(
                BriefType.DAILY,
                LocalDate.of(2026, 11, 24),
                LocalDate.of(2026, 11, 24),
                new CoachContext.ProfileFacts("strength", "beginner", 3, 30),
                new CoachContext.TodayWorkoutFacts(true, false, "Full body", 30, 4),
                new CoachContext.WorkoutFacts(
                        1, 1, 0, 4, BigDecimal.valueOf(160), null, List.of()),
                new CoachContext.WellnessFacts(
                        300, 480, 2, 0, 1, 600, 2000, 400,
                        BigDecimal.valueOf(30), BigDecimal.valueOf(50), BigDecimal.valueOf(12)),
                List.of("No camera form score is recorded for this period"),
                List.of("Average sleep: 5h 0m", "Hydration today: 600/2000 ml"));
    }
}
