package com.project.gympilot_backend.coach.service;

import com.project.gympilot_backend.coach.dto.CoachBriefResponse;
import com.project.gympilot_backend.coach.dto.CoachContext;
import com.project.gympilot_backend.coach.dto.GeneratedCoachBrief;
import com.project.gympilot_backend.coach.model.BriefType;
import com.project.gympilot_backend.coach.model.CoachBrief;
import com.project.gympilot_backend.coach.model.CoachFeedback;
import com.project.gympilot_backend.coach.repository.CoachBriefRepository;
import com.project.gympilot_backend.common.exception.ResourceNotFoundException;
import com.project.gympilot_backend.user.model.User;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class CoachBriefService {
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final CoachBriefRepository repository;
    private final CoachContextService contextService;
    private final CoachRecommendationGenerator generator;
    private final ObjectMapper objectMapper;
    private final ZoneId applicationZone;

    public CoachBriefService(
            CoachBriefRepository repository,
            CoachContextService contextService,
            CoachRecommendationGenerator generator,
            ObjectMapper objectMapper,
            @Value("${app.time-zone:Africa/Lagos}") String timeZone
    ) {
        this.repository = repository;
        this.contextService = contextService;
        this.generator = generator;
        this.objectMapper = objectMapper;
        this.applicationZone = ZoneId.of(timeZone);
    }

    public CoachBriefResponse daily(User user, boolean regenerate) {
        return getOrGenerate(user, BriefType.DAILY, regenerate);
    }

    public CoachBriefResponse weekly(User user, boolean regenerate) {
        return getOrGenerate(user, BriefType.WEEKLY, regenerate);
    }

    @Transactional
    public CoachBriefResponse recordFeedback(
            User user,
            UUID briefId,
            CoachFeedback feedback
    ) {
        CoachBrief brief = repository.findByIdAndUserId(briefId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Coaching brief not found"));
        brief.setFeedback(feedback);
        brief.setFeedbackAt(Instant.now());
        return response(repository.save(brief));
    }

    private CoachBriefResponse getOrGenerate(User user, BriefType type, boolean regenerate) {
        TimeWindow window = currentWindow(type);
        if (!regenerate) {
            CoachBrief existing = repository
                    .findFirstByUserIdAndBriefTypeAndGeneratedAtBetweenOrderByGeneratedAtDesc(
                            user.getId(), type, window.from(), window.to())
                    .orElse(null);
            if (existing != null) return response(existing);
        }

        CoachContext context = contextService.build(user, type);
        GeneratedCoachBrief generated = generator.generate(context);
        Instant generatedAt = Instant.now();
        CoachBrief brief = CoachBrief.builder()
                .user(user)
                .briefType(type)
                .summary(generated.summary())
                .focusToday(generated.focusToday())
                .cautions(write(generated.cautions()))
                .contextSnapshot(write(new StoredContext(generated.generationSource(), context)))
                .generatedAt(generatedAt)
                .build();
        return response(repository.save(brief));
    }

    private CoachBriefResponse response(CoachBrief brief) {
        List<String> cautions = readList(brief.getCautions());
        StoredContext stored = readContext(brief.getContextSnapshot());
        return new CoachBriefResponse(
                brief.getId(),
                brief.getBriefType(),
                brief.getSummary(),
                brief.getFocusToday(),
                cautions,
                stored == null ? List.of() : stored.context().evidence(),
                stored == null ? List.of("The original context snapshot is unavailable")
                        : stored.context().missingData(),
                stored == null ? "UNKNOWN" : stored.generationSource(),
                brief.getFeedback(),
                brief.getFeedbackAt(),
                brief.getGeneratedAt());
    }

    private TimeWindow currentWindow(BriefType type) {
        LocalDate today = LocalDate.now(applicationZone);
        LocalDate start = type == BriefType.DAILY
                ? today
                : today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate end = type == BriefType.DAILY ? today.plusDays(1) : start.plusDays(7);
        return new TimeWindow(
                start.atStartOfDay(applicationZone).toInstant(),
                end.atStartOfDay(applicationZone).toInstant());
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Coach brief could not be serialised", exception);
        }
    }

    private List<String> readList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JacksonException exception) {
            return List.of(json);
        }
    }

    private StoredContext readContext(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, StoredContext.class);
        } catch (JacksonException exception) {
            return null;
        }
    }

    private record TimeWindow(Instant from, Instant to) {
    }

    private record StoredContext(String generationSource, CoachContext context) {
    }
}
