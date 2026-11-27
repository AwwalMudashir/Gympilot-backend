package com.project.gympilot_backend.coach.service;

import com.project.gympilot_backend.coach.dto.CoachContext;
import com.project.gympilot_backend.coach.dto.GeneratedCoachBrief;
import com.project.gympilot_backend.coach.model.BriefType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class CoachRecommendationGenerator {
    private static final int MAX_SUMMARY_LENGTH = 600;
    private static final int MAX_FOCUS_LENGTH = 400;
    private static final int MAX_CAUTION_LENGTH = 240;

    private final ObjectProvider<ChatModel> chatModelProvider;
    private final ObjectMapper objectMapper;
    private final boolean aiEnabled;

    public CoachRecommendationGenerator(
            ObjectProvider<ChatModel> chatModelProvider,
            ObjectMapper objectMapper,
            @Value("${app.ai.coach-enabled:false}") boolean aiEnabled
    ) {
        this.chatModelProvider = chatModelProvider;
        this.objectMapper = objectMapper;
        this.aiEnabled = aiEnabled;
    }

    public GeneratedCoachBrief generate(CoachContext context) {
        GeneratedCoachBrief fallback = fallback(context);
        ChatModel chatModel = aiEnabled ? chatModelProvider.getIfAvailable() : null;
        if (chatModel == null) return fallback;

        try {
            String response = ChatClient.create(chatModel)
                    .prompt()
                    .system(systemPrompt(context.briefType()))
                    .user(objectMapper.writeValueAsString(context))
                    .call()
                    .content();
            GeneratedContent content = parse(response);
            if (!isUsable(content)) return fallback;
            return new GeneratedCoachBrief(
                    compact(content.summary(), MAX_SUMMARY_LENGTH),
                    compact(content.focusToday(), MAX_FOCUS_LENGTH),
                    content.cautions().stream()
                            .filter(value -> value != null && !value.isBlank())
                            .limit(5)
                            .map(value -> compact(value, MAX_CAUTION_LENGTH))
                            .toList(),
                    "OLLAMA");
        } catch (RuntimeException exception) {
            log.warn("AI coach generation failed; using factual fallback: {}", exception.getMessage());
            return fallback;
        }
    }

    private String systemPrompt(BriefType type) {
        String periodInstruction = type == BriefType.DAILY
                ? "Focus on today's readiness, planned workout, hydration and recovery."
                : "Review the supplied week and suggest one conservative focus for the next few days.";
        return "You are GymBro's conservative fitness coach. Use only facts in the supplied JSON. "
                + periodInstruction + " Never invent measurements or claim that missing data is normal. "
                + "Do not diagnose, prescribe treatment or medication, make medical claims, or give extreme diet advice. "
                + "Do not change the workout plan. If recovery data is poor, recommend reducing intensity or resting "
                + "and advise seeking qualified help only when appropriate. Return JSON only with this exact shape: "
                + "{\"summary\":\"one or two short sentences\",\"focusToday\":\"one actionable sentence\","
                + "\"cautions\":[\"zero to three short cautions\"]}. Do not include markdown.";
    }

    private GeneratedContent parse(String response) {
        if (response == null || response.isBlank()) return null;
        String json = response.strip();
        if (json.startsWith("```")) {
            json = json.replaceFirst("^```(?:json)?\\s*", "")
                    .replaceFirst("\\s*```$", "");
        }
        return objectMapper.readValue(json, GeneratedContent.class);
    }

    private boolean isUsable(GeneratedContent content) {
        if (content == null || content.summary() == null || content.summary().isBlank()
                || content.focusToday() == null || content.focusToday().isBlank()
                || content.cautions() == null) {
            return false;
        }
        String combined = (content.summary() + " " + content.focusToday() + " "
                + String.join(" ", content.cautions())).toLowerCase(Locale.ROOT);
        return !combined.contains("diagnos")
                && !combined.contains("prescribe")
                && !combined.contains("medication")
                && !combined.contains("starve")
                && !combined.contains("extreme calorie");
    }

    private GeneratedCoachBrief fallback(CoachContext context) {
        CoachContext.WellnessFacts wellness = context.wellness();
        CoachContext.WorkoutFacts workouts = context.workouts();
        boolean sleepBelowTarget = wellness.averageSleepMinutes() > 0
                && wellness.sleepTargetMinutes() != null
                && wellness.averageSleepMinutes() < Math.round(wellness.sleepTargetMinutes() * 0.8);
        boolean lowEnergy = wellness.averageEnergyLevel() > 0 && wellness.averageEnergyLevel() <= 2;
        boolean hydrationLow = wellness.waterTargetMl() > 0
                && wellness.waterTodayMl() < wellness.waterTargetMl() * 0.5;
        List<String> cautions = new ArrayList<>();

        String summary;
        String focus;
        if (context.briefType() == BriefType.WEEKLY) {
            summary = "You completed " + workouts.completedWorkouts() + " workout"
                    + (workouts.completedWorkouts() == 1 ? "" : "s") + " this week and recorded "
                    + workouts.caloriesBurned().toPlainString() + " workout calories.";
            if (workouts.skippedWorkouts() > 0) {
                focus = "Keep the next session achievable and return to your planned schedule without adding extra work.";
            } else if (workouts.completedWorkouts() > 0) {
                focus = "Continue with your planned schedule and prioritise consistent recovery between sessions.";
            } else {
                focus = "Start with the next planned session and keep the effort comfortable while you rebuild consistency.";
            }
        } else if (sleepBelowTarget || lowEnergy) {
            summary = "Your recorded recovery is below your usual target today, so a conservative approach is appropriate.";
            focus = context.todayWorkout().restDay()
                    ? "Use the recovery day for gentle movement, regular meals, hydration and an earlier wind-down."
                    : "If you train today, keep the effort comfortable and stop if the session does not feel manageable.";
            cautions.add("Avoid increasing workout intensity while sleep or energy remains below target.");
        } else if (context.todayWorkout().planAvailable() && !context.todayWorkout().restDay()) {
            summary = "Your " + context.todayWorkout().title()
                    + " session is planned for today and the available recovery data does not show a clear reason to progress beyond it.";
            focus = "Follow the stored workout as written, use controlled form and complete only the planned volume.";
        } else if (context.todayWorkout().restDay()) {
            summary = "Today is a planned recovery day, which supports consistency across the rest of your week.";
            focus = "Prioritise light movement, hydration and sleep rather than adding an unplanned hard session.";
        } else {
            summary = "There is not enough plan or recovery information for a highly specific coaching brief yet.";
            focus = "Log your sleep and hydration, then generate a workout plan before making training changes.";
        }

        if (hydrationLow) {
            cautions.add("Hydration is below half of today's target; increase it gradually rather than all at once.");
        }
        if (workouts.averageFormScore() != null && workouts.averageFormScore() < 70) {
            cautions.add("Recent camera form scores were below 70%; prioritise control before speed or load.");
        }
        if (!workouts.formFlags().isEmpty()) {
            cautions.add("Review the recorded form guidance: " + String.join(", ", workouts.formFlags()) + ".");
        }
        if (!context.missingData().isEmpty()) {
            cautions.add("This brief is limited because some tracking data is missing.");
        }
        return new GeneratedCoachBrief(summary, focus, List.copyOf(cautions), "FALLBACK");
    }

    private String compact(String value, int maximumLength) {
        String compact = value.strip().replaceAll("\\s+", " ");
        return compact.length() <= maximumLength
                ? compact
                : compact.substring(0, maximumLength).strip();
    }

    private record GeneratedContent(String summary, String focusToday, List<String> cautions) {
    }
}
