package com.project.gympilot_backend.workout.service;

import com.project.gympilot_backend.profile.model.UserProfile;
import com.project.gympilot_backend.workout.model.WorkoutPlanItem;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PlanExplanationService {
    private static final int MAX_EXPLANATION_LENGTH = 600;
    private final ObjectProvider<ChatModel> chatModelProvider;
    private final boolean aiEnabled;

    public PlanExplanationService(
            ObjectProvider<ChatModel> chatModelProvider,
            @Value("${app.ai.plan-explanation-enabled:false}") boolean aiEnabled
    ) {
        this.chatModelProvider = chatModelProvider;
        this.aiEnabled = aiEnabled;
    }

    public String explain(UserProfile profile, List<WorkoutPlanItem> items) {
        String fallback = deterministicExplanation(profile);
        ChatModel chatModel = aiEnabled ? chatModelProvider.getIfAvailable() : null;
        if (chatModel == null) return fallback;

        try {
            String content = ChatClient.create(chatModel)
                    .prompt()
                    .system("You explain an already-generated fitness plan. Use only the supplied facts. "
                            + "Do not change the plan, prescribe new exercises, diagnose conditions, or make medical claims. "
                            + "Return exactly two concise, encouraging sentences and no heading.")
                    .user(buildPrompt(profile, items))
                    .call()
                    .content();
            if (content == null || content.isBlank()) return fallback;
            String compact = content.strip().replaceAll("\\s+", " ");
            return compact.length() <= MAX_EXPLANATION_LENGTH
                    ? compact
                    : compact.substring(0, MAX_EXPLANATION_LENGTH).strip();
        } catch (RuntimeException exception) {
            log.warn("Ollama plan explanation failed; using deterministic explanation");
            return fallback;
        }
    }

    private String buildPrompt(UserProfile profile, List<WorkoutPlanItem> items) {
        String plan = items.stream()
                .map(item -> "day " + item.getDayOfWeek() + ": " + item.getExercise().getName()
                        + " (" + item.getSets() + "x" + item.getTargetRepetitions() + ")")
                .distinct()
                .reduce((left, right) -> left + "; " + right)
                .orElse("No movements");
        return "Goal: " + label(profile.getFitnessGoal().name())
                + "\nExperience: " + label(profile.getExperienceLevel().name())
                + "\nTraining days: " + profile.getDaysPerWeek()
                + "\nSession duration: " + profile.getWorkoutDurationMinutes() + " minutes"
                + "\nAvailable equipment: " + String.join(", ", profile.getAvailableEquipment())
                + "\nGenerated plan: " + plan;
    }

    private String deterministicExplanation(UserProfile profile) {
        return "Built for " + label(profile.getFitnessGoal().name())
                + " with " + profile.getDaysPerWeek() + " training days, "
                + profile.getWorkoutDurationMinutes() + "-minute sessions and your available equipment.";
    }

    private String label(String value) {
        return value.toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
